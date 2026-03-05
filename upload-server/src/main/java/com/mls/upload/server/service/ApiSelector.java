package com.mls.upload.server.service;

import com.mls.upload.server.config.CacheRefreshProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 智能API选择器
 * 根据文件数量和系统配置自动选择最优的缓存刷新API
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Component
public class ApiSelector {

    private static final Logger logger = LoggerFactory.getLogger(ApiSelector.class);

    @Autowired
    private CacheRefreshProperties properties;

    /**
     * API类型枚举
     */
    public enum ApiType {
        SINGLE("single", "/api/cache/update/single"),
        BATCH("batch", "/api/cache/update/batch"),
        LARGE_BATCH("large-batch", "/api/cache/update/large-batch"),
        MEGA_BATCH_ASYNC("mega-batch-async", "/api/cache/update/large-batch-async");

        private final String name;
        private final String endpoint;

        ApiType(String name, String endpoint) {
            this.name = name;
            this.endpoint = endpoint;
        }

        public String getName() {
            return name;
        }

        public String getEndpoint() {
            return endpoint;
        }
    }

    /**
     * API选择结果
     */
    public static class ApiSelection {
        private final ApiType apiType;
        private final String endpoint;
        private final String reason;
        private final boolean isAsync;

        public ApiSelection(ApiType apiType, String endpoint, String reason, boolean isAsync) {
            this.apiType = apiType;
            this.endpoint = endpoint;
            this.reason = reason;
            this.isAsync = isAsync;
        }

        public ApiType getApiType() {
            return apiType;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getReason() {
            return reason;
        }

        public boolean isAsync() {
            return isAsync;
        }

        @Override
        public String toString() {
            return String.format("ApiSelection{type=%s, endpoint='%s', reason='%s', async=%s}",
                    apiType.getName(), endpoint, reason, isAsync);
        }
    }

    /**
     * 根据文件列表智能选择最优API
     *
     * @param filenames 文件名列表
     * @return API选择结果
     */
    public ApiSelection selectOptimalApi(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        int fileCount = filenames.size();
        return selectOptimalApi(fileCount);
    }

    /**
     * 根据文件数量智能选择最优API
     *
     * @param fileCount 文件数量
     * @return API选择结果
     */
    public ApiSelection selectOptimalApi(int fileCount) {
        if (fileCount <= 0) {
            throw new IllegalArgumentException("文件数量必须大于0");
        }

        CacheRefreshProperties.Batch batchConfig = properties.getBatch();

        // 单文件处理
        if (fileCount == 1) {
            return new ApiSelection(
                    ApiType.SINGLE,
                    properties.getTargetUrl() + ApiType.SINGLE.getEndpoint(),
                    "单文件处理",
                    false
            );
        }

        // 小批量处理
        if (fileCount < batchConfig.getLargeBatchThreshold()) {
            return new ApiSelection(
                    ApiType.BATCH,
                    properties.getTargetUrl() + ApiType.BATCH.getEndpoint(),
                    String.format("小批量处理 (%d < %d)", fileCount, batchConfig.getLargeBatchThreshold()),
                    false
            );
        }

        // 超大批量异步处理（如果启用）
        if (fileCount >= batchConfig.getMegaBatchThreshold()) {
            if (batchConfig.isEnableMegaBatchAsync()) {
                return new ApiSelection(
                        ApiType.MEGA_BATCH_ASYNC,
                        properties.getTargetUrl() + ApiType.MEGA_BATCH_ASYNC.getEndpoint(),
                        String.format("超大批量异步处理 (%d >= %d)", fileCount, batchConfig.getMegaBatchThreshold()),
                        true
                );
            } else {
                // 如果禁用了超大批量异步API，降级到大批量处理
                return new ApiSelection(
                        ApiType.LARGE_BATCH,
                        properties.getTargetUrl() + ApiType.LARGE_BATCH.getEndpoint(),
                        String.format("超大批量降级到大批量处理 (%d >= %d, 异步API已禁用)",
                                fileCount, batchConfig.getMegaBatchThreshold()),
                        false
                );
            }
        }

        // 大批量处理
        return new ApiSelection(
                ApiType.LARGE_BATCH,
                properties.getTargetUrl() + ApiType.LARGE_BATCH.getEndpoint(),
                String.format("大批量处理 (%d >= %d, < %d)", 
                        fileCount, batchConfig.getLargeBatchThreshold(), batchConfig.getMegaBatchThreshold()),
                false
        );
    }

    /**
     * 检查是否需要分批处理
     *
     * @param fileCount 文件数量
     * @return 是否需要分批
     */
    public boolean needsBatching(int fileCount) {
        return fileCount > properties.getBatch().getMaxBatchSize();
    }

    /**
     * 检查是否需要强制分批处理（超过最大单次处理限制）
     *
     * @param fileCount 文件数量
     * @return 是否需要强制分批
     */
    public boolean needsForcedBatching(int fileCount) {
        return fileCount > properties.getBatch().getMaxSingleBatchSize();
    }

    /**
     * 计算最优批次大小
     *
     * @param fileCount 总文件数量
     * @return 最优批次大小
     */
    public int calculateOptimalBatchSize(int fileCount) {
        CacheRefreshProperties.Batch batchConfig = properties.getBatch();

        if (fileCount <= batchConfig.getOptimalBatchSize()) {
            return fileCount;
        }

        // 根据文件数量动态调整批次大小
        if (fileCount <= 1000) {
            return batchConfig.getDefaultBatchSize();
        } else if (fileCount <= 3000) {
            return batchConfig.getOptimalBatchSize();
        } else {
            return Math.min(batchConfig.getMaxBatchSize(), 
                    Math.max(batchConfig.getOptimalBatchSize(), fileCount / batchConfig.getMaxConcurrentBatches()));
        }
    }

    /**
     * 获取API选择统计信息
     *
     * @param fileCount 文件数量
     * @return 统计信息字符串
     */
    public String getSelectionStats(int fileCount) {
        ApiSelection selection = selectOptimalApi(fileCount);
        return String.format("[API选择] 文件数量=%d, 选择API=%s, 原因=%s, 异步=%s",
                fileCount, selection.getApiType().getName(), selection.getReason(), selection.isAsync());
    }

    /**
     * 验证API选择配置
     *
     * @return 配置是否有效
     */
    public boolean validateConfiguration() {
        CacheRefreshProperties.Batch batchConfig = properties.getBatch();

        if (batchConfig.getThreshold() <= 0) {
            logger.error("批量处理阈值必须大于0: {}", batchConfig.getThreshold());
            return false;
        }

        if (batchConfig.getLargeBatchThreshold() <= batchConfig.getThreshold()) {
            logger.error("大批量阈值必须大于批量阈值: {} <= {}", 
                    batchConfig.getLargeBatchThreshold(), batchConfig.getThreshold());
            return false;
        }

        if (batchConfig.getMegaBatchThreshold() <= batchConfig.getLargeBatchThreshold()) {
            logger.error("超大批量阈值必须大于大批量阈值: {} <= {}", 
                    batchConfig.getMegaBatchThreshold(), batchConfig.getLargeBatchThreshold());
            return false;
        }

        if (batchConfig.getMaxBatchSize() <= 0) {
            logger.error("最大批处理大小必须大于0: {}", batchConfig.getMaxBatchSize());
            return false;
        }

        logger.info("API选择器配置验证通过");
        return true;
    }
}
