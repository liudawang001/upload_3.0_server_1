package com.mls.upload.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控工具类
 * 用于监控Docker特征提取服务的性能指标
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class PerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    // 存储计时器的开始时间
    private static final ThreadLocal<ConcurrentHashMap<String, Long>> timers = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // 性能统计数据
    private static final ConcurrentHashMap<String, PerformanceStats> stats = new ConcurrentHashMap<>();

    /**
     * 性能统计数据结构
     */
    public static class PerformanceStats {
        private final AtomicLong totalCount = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);

        public long getTotalCount() {
            return totalCount.get();
        }

        public long getTotalTime() {
            return totalTime.get();
        }

        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMaxTime() {
            return maxTime.get();
        }

        public long getErrorCount() {
            return errorCount.get();
        }

        public double getAverageTime() {
            long count = totalCount.get();
            return count > 0 ? (double) totalTime.get() / count : 0.0;
        }

        public double getSuccessRate() {
            long total = totalCount.get();
            return total > 0 ? (double) (total - errorCount.get()) / total * 100 : 0.0;
        }

        void addTime(long time) {
            totalCount.incrementAndGet();
            totalTime.addAndGet(time);
            
            // 更新最小时间
            long currentMin = minTime.get();
            while (time < currentMin && !minTime.compareAndSet(currentMin, time)) {
                currentMin = minTime.get();
            }
            
            // 更新最大时间
            long currentMax = maxTime.get();
            while (time > currentMax && !maxTime.compareAndSet(currentMax, time)) {
                currentMax = maxTime.get();
            }
        }

        void addError() {
            errorCount.incrementAndGet();
        }

        void incrementCount() {
            totalCount.incrementAndGet();
        }

        @Override
        public String toString() {
            return String.format(
                "PerformanceStats{count=%d, avgTime=%.2fms, minTime=%dms, maxTime=%dms, errorCount=%d, successRate=%.2f%%}",
                getTotalCount(), getAverageTime(), getMinTime(), getMaxTime(), getErrorCount(), getSuccessRate()
            );
        }
    }

    /**
     * 开始计时
     * 
     * @param timerName 计时器名称
     */
    public static void startTimer(String timerName) {
        timers.get().put(timerName, System.currentTimeMillis());
        logger.debug("开始计时: {}", timerName);
    }

    /**
     * 结束计时并记录性能数据
     * 
     * @param timerName 计时器名称
     * @return 执行时间（毫秒）
     */
    public static long endTimer(String timerName) {
        Long startTime = timers.get().remove(timerName);
        if (startTime == null) {
            logger.warn("计时器未找到: {}", timerName);
            return 0;
        }

        long duration = System.currentTimeMillis() - startTime;
        
        // 记录性能统计
        stats.computeIfAbsent(timerName, k -> new PerformanceStats()).addTime(duration);
        
        logger.debug("结束计时: {}, 耗时: {}ms", timerName, duration);
        return duration;
    }

    /**
     * 记录错误
     *
     * @param timerName 计时器名称
     */
    public static void recordError(String timerName) {
        stats.computeIfAbsent(timerName, k -> new PerformanceStats()).addError();
        logger.debug("记录错误: {}", timerName);
    }

    /**
     * 记录成功操作
     *
     * @param timerName 计时器名称
     */
    public static void recordSuccess(String timerName) {
        stats.computeIfAbsent(timerName, k -> new PerformanceStats()).incrementCount();
        logger.debug("记录成功: {}", timerName);
    }

    /**
     * 获取性能统计数据
     * 
     * @param timerName 计时器名称
     * @return 性能统计数据
     */
    public static PerformanceStats getStats(String timerName) {
        return stats.get(timerName);
    }

    /**
     * 获取所有性能统计数据
     * 
     * @return 所有性能统计数据
     */
    public static ConcurrentHashMap<String, PerformanceStats> getAllStats() {
        return new ConcurrentHashMap<>(stats);
    }

    /**
     * 清除指定计时器的统计数据
     * 
     * @param timerName 计时器名称
     */
    public static void clearStats(String timerName) {
        stats.remove(timerName);
        logger.debug("清除统计数据: {}", timerName);
    }

    /**
     * 清除所有统计数据
     */
    public static void clearAllStats() {
        stats.clear();
        logger.debug("清除所有统计数据");
    }

    /**
     * 打印性能报告
     */
    public static void printReport() {
        logger.info("=== 性能监控报告 ===");
        stats.forEach((name, stat) -> {
            logger.info("{}: {}", name, stat);
        });
        logger.info("=== 报告结束 ===");
    }

    /**
     * 获取性能报告字符串
     * 
     * @return 性能报告
     */
    public static String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 性能监控报告 ===\n");
        stats.forEach((name, stat) -> {
            report.append(String.format("%s: %s\n", name, stat));
        });
        report.append("=== 报告结束 ===");
        return report.toString();
    }

    /**
     * 常用的计时器名称常量
     */
    public static class TimerNames {
        public static final String DOCKER_FEATURE_EXTRACTION = "DOCKER_FEATURE_EXTRACTION";
        public static final String DOCKER_HEALTH_CHECK = "DOCKER_HEALTH_CHECK";
        public static final String IMAGE_BASE64_ENCODING = "IMAGE_BASE64_ENCODING";
        public static final String FEATURE_VECTOR_PARSING = "FEATURE_VECTOR_PARSING";
        public static final String DATABASE_SAVE = "DATABASE_SAVE";
        public static final String TOTAL_UPLOAD_PROCESS = "TOTAL_UPLOAD_PROCESS";
        public static final String FEATURE_VECTOR_OVERWRITE = "FEATURE_VECTOR_OVERWRITE";
        public static final String FEATURE_VECTOR_INSERT = "FEATURE_VECTOR_INSERT";
    }
}
