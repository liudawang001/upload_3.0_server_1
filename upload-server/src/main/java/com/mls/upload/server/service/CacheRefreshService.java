package com.mls.upload.server.service;

import com.mls.upload.server.config.CacheRefreshProperties;
import com.mls.upload.server.service.ApiSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存刷新服务
 * 负责调用项目B的缓存更新API，实现缓存同步
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Service
public class CacheRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(CacheRefreshService.class);

    @Autowired
    private CacheRefreshProperties properties;

    @Autowired
    @Qualifier("cacheRefreshRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ApiSelector apiSelector;

    /**
     * 缓存刷新专用线程池
     */
    private ThreadPoolExecutor cacheRefreshExecutor;

    /**
     * 批量收集器，用于收集短时间内的多个文件进行批量处理
     */
    private final ConcurrentLinkedQueue<String> batchQueue = new ConcurrentLinkedQueue<>();

    /**
     * 成功文件收集器，用于收集特征提取成功的文件，等待队列清空后批量刷新
     */
    private final ConcurrentLinkedQueue<String> successfulFilesQueue = new ConcurrentLinkedQueue<>();

    /**
     * 批量处理调度器
     */
    private ScheduledExecutorService batchScheduler;

    /**
     * 成功调用计数器
     */
    private final AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 失败调用计数器
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            logger.info("缓存刷新功能已禁用");
            return;
        }

        // 初始化线程池
        CacheRefreshProperties.ThreadPool threadPoolConfig = properties.getThreadPool();
        this.cacheRefreshExecutor = new ThreadPoolExecutor(
                threadPoolConfig.getCoreSize(),
                threadPoolConfig.getMaxSize(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(threadPoolConfig.getQueueCapacity()),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "cache-refresh-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 初始化批量处理调度器
        this.batchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-refresh-batch-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 启动批量处理定时任务
        batchScheduler.scheduleWithFixedDelay(this::processBatchQueue, 
                properties.getBatch().getCollectTimeout(), 
                properties.getBatch().getCollectTimeout(), 
                TimeUnit.MILLISECONDS);

        logger.info("缓存刷新服务初始化完成: {}", properties);
    }

    @PreDestroy
    public void destroy() {
        if (cacheRefreshExecutor != null) {
            cacheRefreshExecutor.shutdown();
            try {
                if (!cacheRefreshExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    cacheRefreshExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cacheRefreshExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (batchScheduler != null) {
            batchScheduler.shutdown();
            try {
                if (!batchScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    batchScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                batchScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("缓存刷新服务已关闭，成功调用: {}, 失败调用: {}", 
                   successCount.get(), failureCount.get());
    }

    /**
     * 刷新单个文件的缓存
     * 
     * @param filename 文件名
     */
    @Async
    public void refreshSingleFileCache(String filename) {
        if (!properties.isEnabled()) {
            logger.debug("缓存刷新功能已禁用，跳过文件: {}", filename);
            return;
        }

        if (filename == null || filename.trim().isEmpty()) {
            logger.warn("文件名为空，跳过缓存刷新");
            return;
        }

        logger.info("提交单文件缓存刷新任务: {}", filename);

        // 检查是否应该使用批量处理
        if (shouldUseBatchProcessing()) {
            addToBatchQueue(filename);
            return;
        }

        // 提交到线程池异步执行
        cacheRefreshExecutor.submit(() -> {
            try {
                refreshSingleFileCacheInternal(filename);
            } catch (Exception e) {
                logger.error("单文件缓存刷新任务执行异常: filename={}, error={}", filename, e.getMessage(), e);
                failureCount.incrementAndGet();
            }
        });
    }

    /**
     * 批量刷新文件缓存
     * 
     * @param filenames 文件名列表
     */
    @Async
    public void refreshBatchFilesCache(List<String> filenames) {
        if (!properties.isEnabled()) {
            logger.debug("缓存刷新功能已禁用，跳过批量文件: {}", filenames.size());
            return;
        }

        if (filenames == null || filenames.isEmpty()) {
            logger.warn("文件名列表为空，跳过批量缓存刷新");
            return;
        }

        logger.info("提交批量文件缓存刷新任务: {} 个文件", filenames.size());

        // 提交到线程池异步执行
        cacheRefreshExecutor.submit(() -> {
            try {
                refreshBatchFilesCacheInternal(filenames);
            } catch (Exception e) {
                logger.error("批量文件缓存刷新任务执行异常: count={}, error={}", filenames.size(), e.getMessage(), e);
                failureCount.incrementAndGet();
            }
        });
    }

    /**
     * 智能增量缓存刷新
     */
    @Async
    public void smartIncrementalRefresh() {
        if (!properties.isEnabled()) {
            logger.debug("缓存刷新功能已禁用，跳过智能增量刷新");
            return;
        }

        logger.info("提交智能增量缓存刷新任务");

        // 提交到线程池异步执行
        cacheRefreshExecutor.submit(() -> {
            try {
                smartIncrementalRefreshInternal();
            } catch (Exception e) {
                logger.error("智能增量缓存刷新任务执行异常: error={}", e.getMessage(), e);
                failureCount.incrementAndGet();
            }
        });
    }

    /**
     * 收集特征提取成功的文件，用于后续批量缓存刷新
     *
     * @param filename 成功提取特征的文件名
     */
    public void collectSuccessfulFile(String filename) {
        if (!properties.isEnabled()) {
            logger.debug("缓存刷新功能已禁用，跳过收集文件: {}", filename);
            return;
        }

        if (filename == null || filename.trim().isEmpty()) {
            logger.warn("文件名为空，跳过收集");
            return;
        }

        // 避免重复收集同一文件
        if (!successfulFilesQueue.contains(filename)) {
            successfulFilesQueue.offer(filename);
            logger.debug("[缓存刷新] 收集成功文件: {}, 当前收集数量: {}", filename, successfulFilesQueue.size());
        } else {
            logger.debug("[缓存刷新] 文件已存在于收集队列中: {}", filename);
        }
    }

    /**
     * 当队列清空时触发批量缓存刷新
     */
    public void triggerBatchRefreshIfReady() {
        if (!properties.isEnabled()) {
            logger.debug("缓存刷新功能已禁用，跳过批量刷新触发");
            return;
        }

        if (successfulFilesQueue.isEmpty()) {
            logger.debug("[缓存刷新] 没有待刷新的文件，跳过批量刷新");
            return;
        }

        // 收集所有成功的文件
        List<String> filesToRefresh = new java.util.ArrayList<>();
        String filename;
        while ((filename = successfulFilesQueue.poll()) != null) {
            filesToRefresh.add(filename);
        }

        if (!filesToRefresh.isEmpty()) {
            long startTime = System.currentTimeMillis();
            logger.info("[缓存刷新] 队列已清空，开始智能缓存刷新: 文件数量={}", filesToRefresh.size());

            // 检查是否需要强制分批处理
            if (apiSelector.needsForcedBatching(filesToRefresh.size())) {
                logger.warn("[缓存刷新] 文件数量({})超过最大单次处理限制({}), 强制分批处理",
                           filesToRefresh.size(), properties.getBatch().getMaxSingleBatchSize());
                refreshLargeBatchWithSplitting(filesToRefresh, startTime);
                return;
            }

            // 使用智能API选择器选择最优API
            ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(filesToRefresh);
            logger.info("[缓存刷新] {}", apiSelector.getSelectionStats(filesToRefresh.size()));
            logger.info("[缓存刷新] 调用{}API: POST {}",
                    selection.getApiType().getName(), selection.getEndpoint());

            // 根据选择的API类型进行处理
            switch (selection.getApiType()) {
                case SINGLE:
                    refreshSingleFileCacheInternal(filesToRefresh.get(0), startTime);
                    break;
                case BATCH:
                    refreshBatchFilesCacheInternal(filesToRefresh, startTime);
                    break;
                case LARGE_BATCH:
                    refreshLargeBatchFilesCacheInternal(filesToRefresh, startTime);
                    break;
                case MEGA_BATCH_ASYNC:
                    // 尝试超大批量异步处理，失败时降级
                    if (!refreshMegaBatchFilesCacheAsyncWithFallback(filesToRefresh, startTime)) {
                        logger.warn("[缓存刷新] 超大批量异步处理失败，开始降级处理");
                        // 降级到分批大批量处理
                        refreshLargeBatchWithSplitting(filesToRefresh, startTime);
                    }
                    break;
                default:
                    logger.warn("[缓存刷新] 未知的API类型: {}", selection.getApiType());
                    // 降级到批量处理
                    refreshBatchFilesCacheInternal(filesToRefresh, startTime);
                    break;
            }
        }
    }

    /**
     * 大批量文件缓存刷新（内部方法）
     */
    private void refreshLargeBatchFilesCacheInternal(List<String> filenames, long startTime) {
        if (filenames == null || filenames.isEmpty()) {
            logger.warn("大批量刷新文件列表为空");
            return;
        }

        logger.debug("开始大批量刷新文件缓存: count={}, url={}/api/cache/update/large-batch",
                    filenames.size(), properties.getTargetUrl());

        String url = properties.getTargetUrl() + "/api/cache/update/large-batch";

        // 构建请求体
        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("filenames", filenames);
        requestBody.put("batchSize", properties.getBatch().getOptimalBatchSize());
        requestBody.put("enableParallel", properties.getBatch().isEnableParallelProcessing());
        requestBody.put("maxConcurrent", properties.getBatch().getMaxConcurrentBatches());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long delay = properties.getRetry().getInitialDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[缓存刷新] 大批量缓存刷新成功: 耗时={}ms, 成功文件={}",
                               duration, filenames.size());
                    successCount.incrementAndGet();
                    return;
                } else {
                    logger.warn("大批量文件缓存刷新失败: count={}, attempt={}, status={}, response={}",
                               filenames.size(), attempt, response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                logger.warn("大批量文件缓存刷新异常: count={}, attempt={}, error={}",
                           filenames.size(), attempt, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("大批量文件缓存刷新最终失败: count={}, maxAttempts={}", filenames.size(), maxAttempts);
        failureCount.incrementAndGet();
    }

    /**
     * 超大批量异步文件缓存刷新（带降级处理）
     */
    private boolean refreshMegaBatchFilesCacheAsyncWithFallback(List<String> filenames, long startTime) {
        try {
            refreshMegaBatchFilesCacheAsyncInternal(filenames, startTime);
            return true;
        } catch (Exception e) {
            logger.warn("[缓存刷新] 超大批量异步处理异常，准备降级: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 大批量分批处理
     */
    private void refreshLargeBatchWithSplitting(List<String> filenames, long startTime) {
        if (filenames == null || filenames.isEmpty()) {
            logger.warn("分批处理文件列表为空");
            return;
        }

        int totalFiles = filenames.size();
        int batchSize = properties.getBatch().getOptimalBatchSize();
        int totalBatches = (totalFiles + batchSize - 1) / batchSize;

        logger.info("[缓存刷新] 开始分批大批量处理: 总文件数={}, 批次大小={}, 总批次数={}",
                   totalFiles, batchSize, totalBatches);

        int successfulBatches = 0;
        int failedBatches = 0;

        for (int i = 0; i < totalBatches; i++) {
            int startIndex = i * batchSize;
            int endIndex = Math.min(startIndex + batchSize, totalFiles);
            List<String> batchFiles = filenames.subList(startIndex, endIndex);

            logger.info("[缓存刷新] 处理批次 {}/{}: 文件数={}", i + 1, totalBatches, batchFiles.size());

            try {
                refreshLargeBatchFilesCacheInternal(batchFiles, System.currentTimeMillis());
                successfulBatches++;
                logger.debug("[缓存刷新] 批次 {}/{} 处理成功", i + 1, totalBatches);
            } catch (Exception e) {
                failedBatches++;
                logger.warn("[缓存刷新] 批次 {}/{} 处理失败: error={}", i + 1, totalBatches, e.getMessage());
            }

            // 批次间短暂延迟，避免服务器压力过大
            if (i < totalBatches - 1) {
                try {
                    Thread.sleep(properties.getBatch().getBatchDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        logger.info("[缓存刷新] 分批大批量处理完成: 总耗时={}ms, 成功批次={}, 失败批次={}, 总批次={}",
                   totalDuration, successfulBatches, failedBatches, totalBatches);

        if (successfulBatches > 0) {
            successCount.incrementAndGet();
        }
        if (failedBatches > 0) {
            failureCount.incrementAndGet();
        }
    }

    /**
     * 超大批量异步文件缓存刷新（内部方法）
     */
    private void refreshMegaBatchFilesCacheAsyncInternal(List<String> filenames, long startTime) {
        if (filenames == null || filenames.isEmpty()) {
            logger.warn("超大批量刷新文件列表为空");
            return;
        }

        logger.debug("开始超大批量异步刷新文件缓存: count={}, url={}/api/cache/update/large-batch-async",
                    filenames.size(), properties.getTargetUrl());

        String url = properties.getTargetUrl() + "/api/cache/update/large-batch-async";

        // 构建请求体
        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("filenames", filenames);
        requestBody.put("batchSize", properties.getBatch().getOptimalBatchSize());
        requestBody.put("enableParallel", properties.getBatch().isEnableParallelProcessing());
        requestBody.put("maxConcurrent", properties.getBatch().getMaxConcurrentBatches());
        requestBody.put("enableProgressTracking", properties.getBatch().isEnableProgressTracking());
        requestBody.put("progressReportInterval", properties.getBatch().getProgressReportInterval());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long delay = properties.getRetry().getInitialDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[缓存刷新] 超大批量异步缓存刷新已提交: 耗时={}ms, 文件数量={}",
                               duration, filenames.size());
                    logger.info("[缓存刷新] 异步处理响应: {}", response.getBody());
                    successCount.incrementAndGet();
                    return;
                } else {
                    logger.warn("超大批量异步文件缓存刷新失败: count={}, attempt={}, status={}, response={}",
                               filenames.size(), attempt, response.getStatusCode(), response.getBody());

                    // 如果是500错误，记录更详细的信息
                    if (response.getStatusCode().is5xxServerError()) {
                        logger.error("[缓存刷新] 项目B服务器内部错误: status={}, 可能原因: 1)数据量过大 2)内存不足 3)服务负载过高",
                                   response.getStatusCode());
                    }
                }
            } catch (Exception e) {
                logger.warn("超大批量异步文件缓存刷新异常: count={}, attempt={}, error={}",
                           filenames.size(), attempt, e.getMessage());

                // 记录异常类型以便诊断
                logger.debug("[缓存刷新] 异常详情: type={}, message={}",
                           e.getClass().getSimpleName(), e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("超大批量异步文件缓存刷新最终失败: count={}, maxAttempts={}", filenames.size(), maxAttempts);
        failureCount.incrementAndGet();
    }

    /**
     * 获取缓存刷新统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format("缓存刷新统计 - 成功: %d, 失败: %d, 队列大小: %d, 待刷新文件: %d, 活跃线程: %d",
                           successCount.get(), failureCount.get(),
                           batchQueue.size(), successfulFilesQueue.size(),
                           cacheRefreshExecutor != null ? cacheRefreshExecutor.getActiveCount() : 0);
    }

    /**
     * 检查是否应该使用批量处理
     */
    private boolean shouldUseBatchProcessing() {
        // 如果当前队列中已有文件，说明可能是批量上传
        return !batchQueue.isEmpty();
    }

    /**
     * 添加文件到批量队列
     */
    private void addToBatchQueue(String filename) {
        batchQueue.offer(filename);
        logger.debug("文件已添加到批量队列: {}, 当前队列大小: {}", filename, batchQueue.size());
    }

    /**
     * 处理批量队列
     */
    private void processBatchQueue() {
        if (batchQueue.isEmpty()) {
            return;
        }

        // 收集队列中的所有文件
        List<String> filenames = new java.util.ArrayList<>();
        String filename;
        while ((filename = batchQueue.poll()) != null) {
            filenames.add(filename);
        }

        if (!filenames.isEmpty()) {
            logger.info("处理批量队列: {} 个文件", filenames.size());

            if (filenames.size() >= properties.getBatch().getThreshold()) {
                // 使用批量API
                refreshBatchFilesCacheInternal(filenames);
            } else {
                // 使用单文件API
                for (String file : filenames) {
                    refreshSingleFileCacheInternal(file);
                }
            }
        }
    }

    /**
     * 内部方法：刷新单个文件缓存
     */
    private void refreshSingleFileCacheInternal(String filename) {
        refreshSingleFileCacheInternal(filename, System.currentTimeMillis());
    }

    /**
     * 内部方法：刷新单个文件缓存（带性能监控）
     */
    private void refreshSingleFileCacheInternal(String filename, long startTime) {
        String url = properties.getTargetUrl() + "/api/cache/update/single";

        logger.debug("开始刷新单文件缓存: filename={}, url={}", filename, url);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long delay = properties.getRetry().getInitialDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 构建请求URL
                String requestUrl = url + "?filename=" + filename;

                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                // 发送POST请求
                ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[缓存刷新] 单文件缓存刷新成功: 耗时={}ms, 成功文件=1, filename={}",
                               duration, filename);
                    successCount.incrementAndGet();
                    return;
                } else {
                    logger.warn("单文件缓存刷新失败: filename={}, attempt={}, status={}, response={}",
                               filename, attempt, response.getStatusCode(), response.getBody());
                }

            } catch (Exception e) {
                logger.warn("单文件缓存刷新异常: filename={}, attempt={}, error={}",
                           filename, attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    logger.error("单文件缓存刷新最终失败: filename={}, maxAttempts={}", filename, maxAttempts);
                    failureCount.incrementAndGet();
                    return;
                }

                // 指数退避延迟
                try {
                    Thread.sleep(delay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("单文件缓存刷新被中断: filename={}", filename);
                    failureCount.incrementAndGet();
                    return;
                }
            }
        }
    }

    /**
     * 内部方法：批量刷新文件缓存
     */
    private void refreshBatchFilesCacheInternal(List<String> filenames) {
        refreshBatchFilesCacheInternal(filenames, System.currentTimeMillis());
    }

    /**
     * 内部方法：批量刷新文件缓存（带性能监控）
     */
    private void refreshBatchFilesCacheInternal(List<String> filenames, long startTime) {
        String url = properties.getTargetUrl() + "/api/cache/update/batch";

        logger.debug("开始批量刷新文件缓存: count={}, url={}", filenames.size(), url);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long delay = properties.getRetry().getInitialDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<List<String>> request = new HttpEntity<>(filenames, headers);

                // 发送POST请求
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[缓存刷新] 批量缓存刷新成功: 耗时={}ms, 成功文件={}",
                               duration, filenames.size());
                    successCount.incrementAndGet();
                    return;
                } else {
                    logger.warn("批量文件缓存刷新失败: count={}, attempt={}, status={}, response={}",
                               filenames.size(), attempt, response.getStatusCode(), response.getBody());
                }

            } catch (Exception e) {
                logger.warn("批量文件缓存刷新异常: count={}, attempt={}, error={}",
                           filenames.size(), attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    logger.error("批量文件缓存刷新最终失败: count={}, maxAttempts={}", filenames.size(), maxAttempts);
                    failureCount.incrementAndGet();
                    return;
                }

                // 指数退避延迟
                try {
                    Thread.sleep(delay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("批量文件缓存刷新被中断: count={}", filenames.size());
                    failureCount.incrementAndGet();
                    return;
                }
            }
        }
    }

    /**
     * 内部方法：智能增量缓存刷新
     */
    private void smartIncrementalRefreshInternal() {
        String url = properties.getTargetUrl() + "/api/cache/update/incremental";

        logger.debug("开始智能增量缓存刷新: url={}", url);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long delay = properties.getRetry().getInitialDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                // 发送POST请求
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("智能增量缓存刷新成功: attempt={}, response={}",
                               attempt, response.getBody());
                    successCount.incrementAndGet();
                    return;
                } else {
                    logger.warn("智能增量缓存刷新失败: attempt={}, status={}, response={}",
                               attempt, response.getStatusCode(), response.getBody());
                }

            } catch (Exception e) {
                logger.warn("智能增量缓存刷新异常: attempt={}, error={}", attempt, e.getMessage());

                if (attempt == maxAttempts) {
                    logger.error("智能增量缓存刷新最终失败: maxAttempts={}", maxAttempts);
                    failureCount.incrementAndGet();
                    return;
                }

                // 指数退避延迟
                try {
                    Thread.sleep(delay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("智能增量缓存刷新被中断");
                    failureCount.incrementAndGet();
                    return;
                }
            }
        }
    }
}
