package com.mls.upload.server.service.impl;

import com.mls.upload.server.config.FeatureExtractionProperties;
import com.mls.upload.server.service.CacheRefreshService;
import com.mls.upload.server.service.DataService;
import com.mls.upload.server.service.DockerFeatureExtractionService;
import com.mls.upload.server.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.PreDestroy;

/**
 * Docker特征提取服务实现类
 * 实现与Docker容器中特征提取算法的完整交互流程
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@Service
public class DockerFeatureExtractionServiceImpl implements DockerFeatureExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(DockerFeatureExtractionServiceImpl.class);

    @Autowired
    private FeatureExtractionProperties properties;

    @Autowired
    @Qualifier("featureExtractionRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private DataService dataService;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 串行化特征提取执行器
     * 确保Docker服务一次只处理一个请求，避免并发冲突
     */
    private final ExecutorService serialExecutor;

    /**
     * 队列中等待处理的任务计数器
     */
    private final AtomicInteger queueSize = new AtomicInteger(0);

    /**
     * 构造函数 - 初始化串行执行器
     */
    public DockerFeatureExtractionServiceImpl() {
        // 创建单线程执行器，确保特征提取请求串行化处理
        this.serialExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DockerFeatureExtraction-Serial");
                thread.setDaemon(true); // 设置为守护线程
                return thread;
            }
        });

        logger.info("Docker特征提取串行化执行器已初始化");
    }

    @Override
    public boolean isServiceAvailable() {
        if (!properties.isEnabled()) {
            logger.debug("Docker特征提取服务已禁用");
            return false;
        }

        if (!properties.isHealthCheckEnabled()) {
            logger.debug("健康检查已禁用，假设服务可用");
            return true;
        }

        try {
            String healthUrl = properties.getHealthCheckUrl();
            logger.debug("检查Docker服务健康状态: {}", healthUrl);

            PerformanceMonitor.startTimer(PerformanceMonitor.TimerNames.DOCKER_HEALTH_CHECK);
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            PerformanceMonitor.endTimer(PerformanceMonitor.TimerNames.DOCKER_HEALTH_CHECK);

            boolean isAvailable = response.getStatusCode() == HttpStatus.OK;

            if (isAvailable) {
                logger.debug("Docker服务健康检查通过");
            } else {
                logger.warn("Docker服务健康检查失败，状态码: {}", response.getStatusCode());
                PerformanceMonitor.recordError(PerformanceMonitor.TimerNames.DOCKER_HEALTH_CHECK);
            }

            return isAvailable;

        } catch (Exception e) {
            logger.warn("Docker服务健康检查异常: {}，尝试降级检查", e.getMessage());
            PerformanceMonitor.recordError(PerformanceMonitor.TimerNames.DOCKER_HEALTH_CHECK);

            // 降级检查：如果/health端点不存在，尝试检查主API端点的连通性
            return testMainApiConnectivity();
        }
    }

    @Override
    public boolean isOverwriteEnabled() {
        return properties.isOverwriteExisting();
    }

    @Override
    public MultiFeatureVector extractFeatures(File imageFile) {
        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            throw new IllegalArgumentException("图片文件无效: " + 
                (imageFile != null ? imageFile.getAbsolutePath() : "null"));
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return extractFeatures(imageBytes, imageFile.getName());
        } catch (IOException e) {
            logger.error("读取图片文件失败: {}", imageFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Override
    public MultiFeatureVector extractFeatures(byte[] imageBytes, String imageName) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("图片数据为空");
        }

        if (!properties.isEnabled()) {
            logger.info("Docker特征提取服务已禁用，跳过特征提取: {}", imageName);
            return null;
        }

        logger.info("开始提取图片特征: {}, 大小: {} bytes", imageName, imageBytes.length);

        // 开始性能监控
        PerformanceMonitor.startTimer(PerformanceMonitor.TimerNames.DOCKER_FEATURE_EXTRACTION);

        try {
            // 构建Data URI格式的图片数据
            PerformanceMonitor.startTimer(PerformanceMonitor.TimerNames.IMAGE_BASE64_ENCODING);
            String dataUri = buildDataUri(imageBytes, imageName);
            PerformanceMonitor.endTimer(PerformanceMonitor.TimerNames.IMAGE_BASE64_ENCODING);

            // 调用Docker服务
            Map<String, Object> response = callDockerService(dataUri, imageName);

            // 解析响应数据
            PerformanceMonitor.startTimer(PerformanceMonitor.TimerNames.FEATURE_VECTOR_PARSING);
            MultiFeatureVector features = parseFeatureResponse(response);
            PerformanceMonitor.endTimer(PerformanceMonitor.TimerNames.FEATURE_VECTOR_PARSING);

            if (features != null && features.isValid()) {
                logger.info("特征提取成功: {}", imageName);
                PerformanceMonitor.endTimer(PerformanceMonitor.TimerNames.DOCKER_FEATURE_EXTRACTION);
                return features;
            } else {
                logger.warn("特征提取失败，返回数据无效: {}", imageName);
                PerformanceMonitor.recordError(PerformanceMonitor.TimerNames.DOCKER_FEATURE_EXTRACTION);
                return null;
            }

        } catch (Exception e) {
            logger.error("特征提取异常: {}", imageName, e);
            PerformanceMonitor.recordError(PerformanceMonitor.TimerNames.DOCKER_FEATURE_EXTRACTION);
            return null;
        }
    }

    @Override
    public boolean extractAndSaveFeatures(String filename, String imagePath) {
        // 增加队列计数
        int currentQueueSize = queueSize.incrementAndGet();
        logger.info("串行化特征提取任务入队: filename={}, imagePath={}, 队列大小={}",
                   filename, imagePath, currentQueueSize);

        // 使用串行执行器确保Docker服务一次只处理一个请求
        CompletableFuture.runAsync(() -> {
            try {
                // 减少队列计数（任务开始处理）
                int remainingTasks = queueSize.decrementAndGet();
                logger.info("开始串行化特征提取: filename={}, 剩余队列任务={}", filename, remainingTasks);

                File imageFile = new File(imagePath);
                MultiFeatureVector features = extractFeatures(imageFile);

                if (features != null && features.isValid()) {
                    // 转换为BLOB格式并保存
                    byte[] colorBlob = floatArrayToBlob(features.getColor());
                    byte[] glcmBlob = floatArrayToBlob(features.getGlcm());
                    byte[] lbpBlob = floatArrayToBlob(features.getLbp());
                    byte[] vggBlob = floatArrayToBlob(features.getVgg());
                    byte[] vitBlob = floatArrayToBlob(features.getVit());

                    boolean saved = dataService.saveFeatureVector(
                        filename, colorBlob, glcmBlob, lbpBlob, vggBlob, vitBlob);

                    if (saved) {
                        logger.info("串行化特征向量保存成功: filename={}, 剩余队列任务={}",
                                   filename, queueSize.get());

                        // 特征向量保存成功后，收集文件名用于后续批量缓存刷新（仅花型图）
                        try {
                            cacheRefreshService.collectSuccessfulFile(filename);
                            logger.debug("已收集成功文件用于缓存刷新: filename={}", filename);
                        } catch (Exception e) {
                            logger.warn("收集成功文件失败: filename={}, error={}", filename, e.getMessage());
                            // 收集失败不影响特征提取流程
                        }

                        // 检查队列是否已清空，如果是则触发批量缓存刷新
                        int queueRemaining = queueSize.get();
                        if (queueRemaining == 0) {
                            try {
                                cacheRefreshService.triggerBatchRefreshIfReady();
                                logger.debug("队列已清空，已触发批量缓存刷新检查");
                            } catch (Exception e) {
                                logger.warn("触发批量缓存刷新检查失败: error={}", e.getMessage());
                            }
                        }
                    } else {
                        logger.warn("串行化特征向量保存失败: filename={}, 剩余队列任务={}",
                                   filename, queueSize.get());
                    }
                } else {
                    logger.warn("串行化特征提取失败，无法保存: filename={}, 剩余队列任务={}",
                               filename, queueSize.get());
                }

            } catch (Exception e) {
                logger.error("串行化特征提取和保存异常: filename={}, 剩余队列任务={}, error={}",
                           filename, queueSize.get(), e.getMessage(), e);
            }
        }, serialExecutor); // 关键：使用串行执行器

        return true;
    }

    @Override
    public String getHealthStatus() {
        if (!properties.isEnabled()) {
            return "Docker特征提取服务已禁用";
        }

        boolean available = isServiceAvailable();
        int currentQueueSize = queueSize.get();
        String baseStatus = available ? "Docker特征提取服务正常" : "Docker特征提取服务不可用";

        return String.format("%s (串行化队列: %d个任务等待处理)", baseStatus, currentQueueSize);
    }

    /**
     * 获取当前队列状态信息
     * @return 队列状态描述
     */
    public String getQueueStatus() {
        int currentQueueSize = queueSize.get();
        boolean executorRunning = !serialExecutor.isShutdown();

        return String.format("串行化执行器状态: %s, 队列中等待任务: %d个",
                           executorRunning ? "运行中" : "已关闭", currentQueueSize);
    }

    /**
     * 构建Data URI格式的图片数据
     * 根据Docker特征提取方法参考.md的要求
     */
    private String buildDataUri(byte[] imageBytes, String imageName) {
        // Base64编码
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 检测MIME类型
        String mimeType = getMimeType(imageName);

        // 构建Data URI
        String dataUri = String.format("data:%s;base64,%s", mimeType, base64Image);

        logger.debug("构建Data URI完成: {}, MIME类型: {}, Base64长度: {}",
                    imageName, mimeType, base64Image.length());

        return dataUri;
    }

    /**
     * 根据文件名检测MIME类型
     */
    private String getMimeType(String filename) {
        if (filename == null) {
            return "image/jpeg";
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    /**
     * 调用Docker特征提取服务
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callDockerService(String dataUri, String filename) {
        String url = properties.getUrl();
        logger.debug("调用Docker服务: {}", url);

        // 构建multipart/form-data请求
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("Pic", dataUri);

        // 添加详细调试日志
        logger.debug("发送数据: Pic字段长度={}, 数据URI前缀={}",
                    dataUri.length(),
                    dataUri.length() > 50 ? dataUri.substring(0, 50) + "..." : dataUri);

        // 检查数据URI大小
        if (dataUri.length() > 10 * 1024 * 1024) { // 10MB
            logger.warn("数据URI过大: {} bytes, 可能导致Docker服务处理失败", dataUri.length());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Accept", "application/json");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        // 执行请求（带重试机制）
        return executeWithRetry(() -> {
            try {
                @SuppressWarnings("rawtypes")
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    return response.getBody();
                } else {
                    throw new RuntimeException("Docker服务返回错误状态: " + response.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("Docker服务调用异常: filename={}, dataUriLength={}, error={}",
                           filename, dataUri.length(), e.getMessage());
                throw e;
            }
        });
    }

    /**
     * 解析Docker服务响应数据
     */
    @SuppressWarnings("unchecked")
    private MultiFeatureVector parseFeatureResponse(Map<String, Object> response) {
        if (response == null) {
            logger.warn("Docker服务响应为空");
            return null;
        }

        try {
            Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
            if (dataMap == null) {
                logger.warn("响应中缺少data字段");
                return null;
            }

            List<List<Double>> vectorsList = (List<List<Double>>) dataMap.get("vectors");
            if (vectorsList == null || vectorsList.size() != 5) {
                logger.warn("响应中vectors字段无效，期望5个向量，实际: {}",
                           vectorsList != null ? vectorsList.size() : 0);
                return null;
            }

            // 转换为float数组
            float[] colorFeatures = toFloatArray(vectorsList.get(0));
            float[] glcmFeatures = toFloatArray(vectorsList.get(1));
            float[] lbpFeatures = toFloatArray(vectorsList.get(2));
            float[] vggFeatures = toFloatArray(vectorsList.get(3));
            float[] vitFeatures = toFloatArray(vectorsList.get(4));

            return new MultiFeatureVector(colorFeatures, glcmFeatures, lbpFeatures, vggFeatures, vitFeatures);

        } catch (Exception e) {
            logger.error("解析Docker响应数据异常", e);
            return null;
        }
    }

    /**
     * 带重试机制的执行方法（串行化优化版本）
     */
    private <T> T executeWithRetry(Supplier<T> operation) {
        int maxRetries = properties.getRetryTimes();
        int retryInterval = properties.getRetryInterval();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    logger.error("串行化处理：重试{}次后仍然失败，队列剩余任务={}, error={}",
                               maxRetries, queueSize.get(), e.getMessage(), e);
                    throw e;
                }

                logger.warn("串行化处理：第{}次尝试失败，{}ms后重试，队列剩余任务={}, error={}",
                           attempt + 1, retryInterval, queueSize.get(), e.getMessage());

                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("串行化重试等待被中断", ie);
                }
            }
        }

        return null; // 不会到达这里
    }

    /**
     * 将Double列表转换为float数组
     */
    private float[] toFloatArray(List<Double> doubleList) {
        if (doubleList == null) {
            return null;
        }

        float[] floatArray = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            Double value = doubleList.get(i);
            floatArray[i] = value != null ? value.floatValue() : 0.0f;
        }

        return floatArray;
    }

    /**
     * 将float数组转换为BLOB字节数组
     * 根据Docker特征提取方法参考.md的数据转换要求
     */
    private byte[] floatArrayToBlob(float[] array) {
        if (array == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float value : array) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    /**
     * 测试主API端点的连通性（降级健康检查）
     * 当/health端点不可用时使用此方法
     */
    private boolean testMainApiConnectivity() {
        try {
            String apiUrl = properties.getUrl();
            logger.debug("尝试降级健康检查，测试主API连通性: {}", apiUrl);

            // 发送一个简单的HEAD请求测试连通性
            // 注意：这里不发送实际的图片数据，只是测试服务是否响应
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // 使用较短的超时时间进行连通性测试
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.HEAD, requestEntity, String.class);

            // 如果能收到响应（即使是405 Method Not Allowed也说明服务在运行）
            HttpStatus statusCode = response.getStatusCode();
            boolean isConnectable = statusCode != HttpStatus.NOT_FOUND &&
                                  statusCode != HttpStatus.SERVICE_UNAVAILABLE;

            if (isConnectable) {
                logger.debug("降级健康检查通过，主API端点可连接，状态码: {}", statusCode);
            } else {
                logger.warn("降级健康检查失败，主API端点不可用，状态码: {}", statusCode);
            }

            return isConnectable;

        } catch (Exception e) {
            logger.warn("降级健康检查异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将BLOB字节数组转换为float数组（用于验证）
     */
    private float[] blobToFloatArray(byte[] blob) {
        if (blob == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(blob);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        float[] array = new float[blob.length / 4];
        for (int i = 0; i < array.length; i++) {
            array[i] = buffer.getFloat();
        }

        return array;
    }

    /**
     * 资源清理方法
     * 在Spring容器销毁时自动调用，确保执行器正确关闭
     */
    @PreDestroy
    public void destroy() {
        logger.info("正在关闭Docker特征提取串行化执行器...");

        if (serialExecutor != null && !serialExecutor.isShutdown()) {
            serialExecutor.shutdown();
            logger.info("Docker特征提取串行化执行器已关闭");
        }

        // 记录最终的队列状态
        int finalQueueSize = queueSize.get();
        if (finalQueueSize > 0) {
            logger.warn("执行器关闭时仍有{}个任务在队列中", finalQueueSize);
        }
    }
}
