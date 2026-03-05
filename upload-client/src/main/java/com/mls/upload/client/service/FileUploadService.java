package com.mls.upload.client.service;

import com.mls.upload.client.util.ConfigUtil;
import com.mls.upload.client.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件上传服务
 * 处理图片文件的批量上传功能
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    // 支持的图片文件扩展名
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tiff", ".tif", ".webp"
    ));

    // 默认的最大文件大小（50MB）
    private static final long DEFAULT_MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final HttpUtil httpUtil;
    private final ExecutorService uploadExecutor;
    private final String serverUrl;

    public FileUploadService() {
        this.httpUtil = new HttpUtil();
        this.uploadExecutor = Executors.newFixedThreadPool(ConfigUtil.getUploadThreadPoolSize());
        this.serverUrl = ConfigUtil.getServerUrl();
        logger.info("文件上传服务初始化完成，线程池大小: {}", ConfigUtil.getUploadThreadPoolSize());
    }

    /**
     * 扫描指定文件夹中的图片文件
     *
     * @param folderPath 文件夹路径
     * @return 图片文件列表
     * @throws Exception 扫描异常
     */
    public List<File> scanImageFiles(String folderPath) throws Exception {
        logger.info("开始扫描图片文件: {}", folderPath);

        if (folderPath == null || folderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹路径不能为空");
        }

        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new IllegalArgumentException("文件夹不存在: " + folderPath);
        }

        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("指定路径不是文件夹: " + folderPath);
        }

        List<File> imageFiles = new ArrayList<>();
        scanImageFilesRecursive(folder, imageFiles);

        // 按文件名排序
        imageFiles.sort(Comparator.comparing(File::getName));

        logger.info("图片文件扫描完成: 文件夹={}, 图片数量={}", folderPath, imageFiles.size());
        return imageFiles;
    }

    /**
     * 递归扫描文件夹中的图片文件
     *
     * @param folder 文件夹
     * @param imageFiles 图片文件列表
     */
    private void scanImageFilesRecursive(File folder, List<File> imageFiles) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子文件夹
                scanImageFilesRecursive(file, imageFiles);
            } else if (file.isFile() && isImageFile(file)) {
                // 检查文件大小
                if (file.length() <= getMaxFileSize()) {
                    imageFiles.add(file);
                } else {
                    logger.warn("图片文件过大，跳过: {} ({}MB)", file.getName(), file.length() / (1024 * 1024));
                }
            }
        }
    }

    /**
     * 检查是否为图片文件
     *
     * @param file 文件
     * @return 是否为图片文件
     */
    private boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        for (String extension : SUPPORTED_IMAGE_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取最大文件大小
     *
     * @return 最大文件大小（字节）
     */
    private long getMaxFileSize() {
        String maxSizeStr = ConfigUtil.getProperty("file.max.size", "50MB");
        try {
            if (maxSizeStr.toLowerCase().endsWith("mb")) {
                return Long.parseLong(maxSizeStr.substring(0, maxSizeStr.length() - 2)) * 1024 * 1024;
            } else if (maxSizeStr.toLowerCase().endsWith("kb")) {
                return Long.parseLong(maxSizeStr.substring(0, maxSizeStr.length() - 2)) * 1024;
            } else {
                return Long.parseLong(maxSizeStr);
            }
        } catch (NumberFormatException e) {
            logger.warn("无效的文件大小配置: {}，使用默认值", maxSizeStr);
            return DEFAULT_MAX_FILE_SIZE;
        }
    }

    /**
     * 统计文件夹中的图片文件信息
     *
     * @param folderPath 文件夹路径
     * @return 统计信息
     */
    public Map<String, Object> getImageFileStatistics(String folderPath) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            List<File> imageFiles = scanImageFiles(folderPath);

            long totalSize = 0;
            Map<String, Integer> extensionCount = new HashMap<>();

            for (File file : imageFiles) {
                totalSize += file.length();

                String extension = getFileExtension(file.getName()).toLowerCase();
                extensionCount.put(extension, extensionCount.getOrDefault(extension, 0) + 1);
            }

            statistics.put("totalCount", imageFiles.size());
            statistics.put("totalSize", totalSize);
            statistics.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
            statistics.put("extensionCount", extensionCount);
            statistics.put("averageSize", imageFiles.isEmpty() ? 0 : totalSize / imageFiles.size());

        } catch (Exception e) {
            logger.error("获取图片文件统计信息失败: {}", e.getMessage(), e);
            statistics.put("error", e.getMessage());
        }

        return statistics;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * 上传单个文件
     *
     * @param file 文件
     * @param productCode 商品编号
     * @param imageType 图片类型（花型图/配色图）
     * @param username 用户名
     * @return 上传结果
     */
    public CompletableFuture<Map<String, Object>> uploadSingleFile(File file, String productCode, String imageType, String username) {
        return uploadSingleFile(file, productCode, imageType, username, null);
    }

    /**
     * 上传单个文件（带Excel数据）
     *
     * @param file 要上传的文件
     * @param productCode 商品编号
     * @param imageType 图片类型（花型图/配色图）
     * @param username 用户名
     * @param excelRowData Excel行数据（用于花型图上传时同步插入pic_info）
     * @return 上传结果
     */
    public CompletableFuture<Map<String, Object>> uploadSingleFile(File file, String productCode, String imageType, String username, com.mls.upload.client.model.entity.ExcelRowData excelRowData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始上传文件: {} -> {}", file.getName(), productCode);

                // 读取文件内容并转换为Base64
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = com.mls.upload.client.util.Base64Util.encode(fileBytes);

                // 构建JSON上传请求
                Map<String, Object> uploadRequest = new HashMap<>();
                uploadRequest.put("filename", file.getName());
                uploadRequest.put("imageType", imageType); // 直接使用传入的数字代码（1或2）
                uploadRequest.put("base64Data", base64Data);
                uploadRequest.put("username", username);
                uploadRequest.put("needFeatureExtraction", false);

                // 如果是花型图且有Excel数据，添加Excel数据到请求中
                if ("1".equals(imageType) && excelRowData != null) {
                    Map<String, Object> excelData = new HashMap<>();
                    excelData.put("field01", excelRowData.getProductCode());     // 商品编号
                    excelData.put("field02", excelRowData.getCategory());        // 分类
                    excelData.put("field03", excelRowData.getOrderNumber());     // 订单号
                    excelData.put("field04", excelRowData.getFabric());          // 面料
                    excelData.put("field05", excelRowData.getCustomerName());    // 客户名
                    excelData.put("field06", excelRowData.getMarket());          // 市场
                    excelData.put("field07", excelRowData.getSetCount());        // 套数
                    excelData.put("field08", excelRowData.getSoDate());          // SO日期
                    excelData.put("field09", excelRowData.getBulkDate());        // 大货日期
                    excelData.put("field10", excelRowData.getFactoryCode());     // 工厂编号
                    excelData.put("field11", excelRowData.getFactory());         // 工厂
                    excelData.put("field12", excelRowData.getDesignCompany());   // 描稿公司
                    excelData.put("field13", excelRowData.getDesigner());        // 描稿人员
                    excelData.put("field14", excelRowData.getRemark());          // 备注
                    excelData.put("field15", excelRowData.getMerchandiser());    // 理单员
                    excelData.put("field16", excelRowData.getSortOrder());       // 排序

                    uploadRequest.put("excelData", excelData);
                    logger.debug("添加Excel数据到上传请求: productCode={}", excelRowData.getProductCode());
                }

                // 上传文件
                String uploadUrl = serverUrl + "/api/upload/single";
                Map<String, Object> response = httpUtil.postJson(uploadUrl, uploadRequest);

                logger.debug("文件上传完成: {} -> {}, 结果: {}", file.getName(), productCode, response);
                return response;

            } catch (IOException e) {
                logger.error("文件读取失败: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "文件读取失败: " + e.getMessage());
                return errorResponse;
            } catch (Exception e) {
                logger.error("文件上传异常: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "文件上传异常: " + e.getMessage());
                return errorResponse;
            }
        }, uploadExecutor);
    }

    /**
     * 批量上传文件
     *
     * @param uploadTasks 上传任务列表
     * @param username 用户名
     * @param progressCallback 进度回调
     * @return 上传结果列表
     */
    public CompletableFuture<List<Map<String, Object>>> uploadFiles(
            List<UploadTask> uploadTasks,
            String username,
            ProgressCallback progressCallback) {

        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new ArrayList<>();
            int totalTasks = uploadTasks.size();
            int completedTasks = 0;

            logger.info("开始批量上传文件，总数: {}", totalTasks);

            for (UploadTask task : uploadTasks) {
                try {
                    CompletableFuture<Map<String, Object>> uploadFuture =
                        uploadSingleFile(task.getFile(), task.getProductCode(), task.getImageType(), username);

                    Map<String, Object> result = uploadFuture.get();
                    results.add(result);

                    completedTasks++;

                    // 调用进度回调
                    if (progressCallback != null) {
                        double progress = (double) completedTasks / totalTasks;
                        progressCallback.onProgress(completedTasks, totalTasks, progress, task, result);
                    }

                } catch (Exception e) {
                    logger.error("上传任务执行失败: {}", e.getMessage(), e);

                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "上传失败: " + e.getMessage());
                    errorResult.put("file", task.getFile().getName());
                    errorResult.put("productCode", task.getProductCode());
                    results.add(errorResult);

                    completedTasks++;

                    if (progressCallback != null) {
                        double progress = (double) completedTasks / totalTasks;
                        progressCallback.onProgress(completedTasks, totalTasks, progress, task, errorResult);
                    }
                }
            }

            logger.info("批量上传完成，总数: {}, 成功: {}", totalTasks,
                results.stream().mapToInt(r -> Boolean.TRUE.equals(r.get("success")) ? 1 : 0).sum());

            return results;
        }, uploadExecutor);
    }

    /**
     * 验证文件是否可以上传
     *
     * @param file 文件
     * @return 验证结果
     */
    public ValidationResult validateFile(File file) {
        if (file == null) {
            return new ValidationResult(false, "文件不能为空");
        }

        if (!file.exists()) {
            return new ValidationResult(false, "文件不存在: " + file.getName());
        }

        if (!file.isFile()) {
            return new ValidationResult(false, "不是有效的文件: " + file.getName());
        }

        if (!isImageFile(file)) {
            return new ValidationResult(false, "不支持的文件格式: " + file.getName());
        }

        if (file.length() > getMaxFileSize()) {
            return new ValidationResult(false, "文件过大: " + file.getName() +
                " (" + (file.length() / (1024 * 1024)) + "MB)");
        }

        if (file.length() == 0) {
            return new ValidationResult(false, "文件为空: " + file.getName());
        }

        return new ValidationResult(true, "文件验证通过");
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        if (uploadExecutor != null && !uploadExecutor.isShutdown()) {
            uploadExecutor.shutdown();
            logger.info("文件上传服务已关闭");
        }

        if (httpUtil != null) {
            httpUtil.close();
        }
    }

    // 内部类和接口

    /**
     * 上传任务
     */
    public static class UploadTask {
        private File file;
        private String productCode;
        private String imageType;

        public UploadTask(File file, String productCode, String imageType) {
            this.file = file;
            this.productCode = productCode;
            this.imageType = imageType;
        }

        // Getter方法
        public File getFile() { return file; }
        public String getProductCode() { return productCode; }
        public String getImageType() { return imageType; }
    }

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgress(int completed, int total, double progress, UploadTask task, Map<String, Object> result);
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}
