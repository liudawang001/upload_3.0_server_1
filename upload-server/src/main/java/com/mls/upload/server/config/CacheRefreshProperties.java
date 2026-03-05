package com.mls.upload.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存刷新配置属性类
 * 映射application.yml中的cache.refresh配置项
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "cache.refresh")
public class CacheRefreshProperties {

    /**
     * 缓存刷新功能开关
     */
    private boolean enabled = true;

    /**
     * 项目B的服务地址
     */
    private String targetUrl = "http://localhost:8080";

    /**
     * 超时配置
     */
    private Timeout timeout = new Timeout();

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * 批量处理配置
     */
    private Batch batch = new Batch();

    // Getter和Setter方法
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    /**
     * 超时配置内部类
     */
    public static class Timeout {
        /**
         * 连接超时（毫秒）
         */
        private int connect = 5000;

        /**
         * 读取超时（毫秒）
         */
        private int read = 10000;

        public int getConnect() {
            return connect;
        }

        public void setConnect(int connect) {
            this.connect = connect;
        }

        public int getRead() {
            return read;
        }

        public void setRead(int read) {
            this.read = read;
        }
    }

    /**
     * 重试配置内部类
     */
    public static class Retry {
        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始延迟（毫秒）
         */
        private long initialDelay = 1000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }
    }

    /**
     * 线程池配置内部类
     */
    public static class ThreadPool {
        /**
         * 核心线程数
         */
        private int coreSize = 2;

        /**
         * 最大线程数
         */
        private int maxSize = 5;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    /**
     * 批量处理配置内部类
     */
    public static class Batch {
        /**
         * 批量处理阈值（超过此数量使用批量API）
         */
        private int threshold = 5;

        /**
         * 批量收集超时时间（毫秒）
         */
        private long collectTimeout = 2000;

        /**
         * 最大等待队列清空时间（毫秒）
         */
        private long maxWaitTimeout = 30000;

        /**
         * 大批量处理阈值（超过此数量使用大批量API）
         */
        private int largeBatchThreshold = 500;

        /**
         * 超大批量处理阈值（超过此数量使用异步API）
         */
        private int megaBatchThreshold = 2000;

        /**
         * 最大批处理大小
         */
        private int maxBatchSize = 5000;

        /**
         * 默认批处理大小
         */
        private int defaultBatchSize = 500;

        /**
         * 最优批处理大小
         */
        private int optimalBatchSize = 1000;

        /**
         * 最大并发批次数
         */
        private int maxConcurrentBatches = 3;

        /**
         * 是否启用并行处理
         */
        private boolean enableParallelProcessing = true;

        /**
         * 内存使用阈值（MB）
         */
        private int memoryThresholdMb = 512;

        /**
         * 是否启用内存监控
         */
        private boolean enableMemoryMonitoring = true;

        /**
         * 单个批次超时时间（秒）
         */
        private int batchTimeoutSeconds = 600;

        /**
         * 总处理超时时间（秒）
         */
        private int totalTimeoutSeconds = 3600;

        /**
         * 是否允许部分成功
         */
        private boolean enablePartialSuccess = true;

        /**
         * 是否启用进度跟踪
         */
        private boolean enableProgressTracking = true;

        /**
         * 进度报告间隔（处理条数）
         */
        private int progressReportInterval = 100;

        /**
         * 是否启用超大批量异步API
         */
        private boolean enableMegaBatchAsync = true;

        /**
         * 是否启用降级处理
         */
        private boolean enableFallback = true;

        /**
         * 分批处理时的批次间延迟（毫秒）
         */
        private long batchDelayMs = 100;

        /**
         * 最大单次处理文件数（超过此数量强制分批）
         */
        private int maxSingleBatchSize = 3000;

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public long getCollectTimeout() {
            return collectTimeout;
        }

        public void setCollectTimeout(long collectTimeout) {
            this.collectTimeout = collectTimeout;
        }

        public long getMaxWaitTimeout() {
            return maxWaitTimeout;
        }

        public void setMaxWaitTimeout(long maxWaitTimeout) {
            this.maxWaitTimeout = maxWaitTimeout;
        }

        public int getLargeBatchThreshold() {
            return largeBatchThreshold;
        }

        public void setLargeBatchThreshold(int largeBatchThreshold) {
            this.largeBatchThreshold = largeBatchThreshold;
        }

        public int getMegaBatchThreshold() {
            return megaBatchThreshold;
        }

        public void setMegaBatchThreshold(int megaBatchThreshold) {
            this.megaBatchThreshold = megaBatchThreshold;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public int getDefaultBatchSize() {
            return defaultBatchSize;
        }

        public void setDefaultBatchSize(int defaultBatchSize) {
            this.defaultBatchSize = defaultBatchSize;
        }

        public int getOptimalBatchSize() {
            return optimalBatchSize;
        }

        public void setOptimalBatchSize(int optimalBatchSize) {
            this.optimalBatchSize = optimalBatchSize;
        }

        public int getMaxConcurrentBatches() {
            return maxConcurrentBatches;
        }

        public void setMaxConcurrentBatches(int maxConcurrentBatches) {
            this.maxConcurrentBatches = maxConcurrentBatches;
        }

        public boolean isEnableParallelProcessing() {
            return enableParallelProcessing;
        }

        public void setEnableParallelProcessing(boolean enableParallelProcessing) {
            this.enableParallelProcessing = enableParallelProcessing;
        }

        public int getMemoryThresholdMb() {
            return memoryThresholdMb;
        }

        public void setMemoryThresholdMb(int memoryThresholdMb) {
            this.memoryThresholdMb = memoryThresholdMb;
        }

        public boolean isEnableMemoryMonitoring() {
            return enableMemoryMonitoring;
        }

        public void setEnableMemoryMonitoring(boolean enableMemoryMonitoring) {
            this.enableMemoryMonitoring = enableMemoryMonitoring;
        }

        public int getBatchTimeoutSeconds() {
            return batchTimeoutSeconds;
        }

        public void setBatchTimeoutSeconds(int batchTimeoutSeconds) {
            this.batchTimeoutSeconds = batchTimeoutSeconds;
        }

        public int getTotalTimeoutSeconds() {
            return totalTimeoutSeconds;
        }

        public void setTotalTimeoutSeconds(int totalTimeoutSeconds) {
            this.totalTimeoutSeconds = totalTimeoutSeconds;
        }

        public boolean isEnablePartialSuccess() {
            return enablePartialSuccess;
        }

        public void setEnablePartialSuccess(boolean enablePartialSuccess) {
            this.enablePartialSuccess = enablePartialSuccess;
        }

        public boolean isEnableProgressTracking() {
            return enableProgressTracking;
        }

        public void setEnableProgressTracking(boolean enableProgressTracking) {
            this.enableProgressTracking = enableProgressTracking;
        }

        public int getProgressReportInterval() {
            return progressReportInterval;
        }

        public void setProgressReportInterval(int progressReportInterval) {
            this.progressReportInterval = progressReportInterval;
        }

        public boolean isEnableMegaBatchAsync() {
            return enableMegaBatchAsync;
        }

        public void setEnableMegaBatchAsync(boolean enableMegaBatchAsync) {
            this.enableMegaBatchAsync = enableMegaBatchAsync;
        }

        public boolean isEnableFallback() {
            return enableFallback;
        }

        public void setEnableFallback(boolean enableFallback) {
            this.enableFallback = enableFallback;
        }

        public long getBatchDelayMs() {
            return batchDelayMs;
        }

        public void setBatchDelayMs(long batchDelayMs) {
            this.batchDelayMs = batchDelayMs;
        }

        public int getMaxSingleBatchSize() {
            return maxSingleBatchSize;
        }

        public void setMaxSingleBatchSize(int maxSingleBatchSize) {
            this.maxSingleBatchSize = maxSingleBatchSize;
        }
    }

    @Override
    public String toString() {
        return "CacheRefreshProperties{" +
                "enabled=" + enabled +
                ", targetUrl='" + targetUrl + '\'' +
                ", timeout=" + timeout +
                ", retry=" + retry +
                ", threadPool=" + threadPool +
                ", batch=" + batch +
                '}';
    }
}
