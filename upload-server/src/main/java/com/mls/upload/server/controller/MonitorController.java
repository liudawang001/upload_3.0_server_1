package com.mls.upload.server.controller;

import com.mls.upload.server.dto.ApiResponse;
import com.mls.upload.server.service.DockerFeatureExtractionService;
import com.mls.upload.server.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控控制器
 * 提供系统监控和性能统计API
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private DockerFeatureExtractionService dockerFeatureExtractionService;

    /**
     * 获取系统健康状态
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getHealthStatus() {
        logger.info("获取系统健康状态");

        try {
            Map<String, Object> healthInfo = new HashMap<>();
            
            // Docker特征提取服务状态
            boolean dockerServiceAvailable = dockerFeatureExtractionService.isServiceAvailable();
            String dockerHealthStatus = dockerFeatureExtractionService.getHealthStatus();
            
            healthInfo.put("dockerServiceAvailable", dockerServiceAvailable);
            healthInfo.put("dockerHealthStatus", dockerHealthStatus);
            healthInfo.put("systemTime", System.currentTimeMillis());
            healthInfo.put("status", dockerServiceAvailable ? "UP" : "DOWN");
            
            return ApiResponse.success("健康状态获取成功", healthInfo);
            
        } catch (Exception e) {
            logger.error("获取健康状态异常", e);
            return ApiResponse.error("获取健康状态失败");
        }
    }

    /**
     * 获取性能统计信息
     * 
     * @return 性能统计数据
     */
    @GetMapping("/performance")
    public ApiResponse<Map<String, Object>> getPerformanceStats() {
        logger.info("获取性能统计信息");

        try {
            Map<String, Object> performanceInfo = new HashMap<>();
            
            // 获取所有性能统计数据
            ConcurrentHashMap<String, PerformanceMonitor.PerformanceStats> allStats = 
                PerformanceMonitor.getAllStats();
            
            Map<String, Map<String, Object>> statsMap = new HashMap<>();
            allStats.forEach((name, stats) -> {
                Map<String, Object> statMap = new HashMap<>();
                statMap.put("totalCount", stats.getTotalCount());
                statMap.put("totalTime", stats.getTotalTime());
                statMap.put("averageTime", stats.getAverageTime());
                statMap.put("minTime", stats.getMinTime());
                statMap.put("maxTime", stats.getMaxTime());
                statMap.put("errorCount", stats.getErrorCount());
                statMap.put("successRate", stats.getSuccessRate());
                statsMap.put(name, statMap);
            });
            
            performanceInfo.put("statistics", statsMap);
            performanceInfo.put("reportTime", System.currentTimeMillis());
            
            return ApiResponse.success("性能统计获取成功", performanceInfo);
            
        } catch (Exception e) {
            logger.error("获取性能统计异常", e);
            return ApiResponse.error("获取性能统计失败");
        }
    }

    /**
     * 获取性能报告
     * 
     * @return 性能报告文本
     */
    @GetMapping("/performance/report")
    public ApiResponse<String> getPerformanceReport() {
        logger.info("获取性能报告");

        try {
            String report = PerformanceMonitor.getReport();
            return ApiResponse.success("性能报告获取成功", report);
            
        } catch (Exception e) {
            logger.error("获取性能报告异常", e);
            return ApiResponse.error("获取性能报告失败");
        }
    }

    /**
     * 清除性能统计数据
     * 
     * @param timerName 计时器名称，如果为空则清除所有
     * @return 清除结果
     */
    @DeleteMapping("/performance")
    public ApiResponse<Void> clearPerformanceStats(@RequestParam(required = false) String timerName) {
        logger.info("清除性能统计数据: timerName={}", timerName);

        try {
            if (timerName != null && !timerName.trim().isEmpty()) {
                PerformanceMonitor.clearStats(timerName.trim());
                logger.info("已清除指定计时器统计数据: {}", timerName);
            } else {
                PerformanceMonitor.clearAllStats();
                logger.info("已清除所有性能统计数据");
            }
            
            return ApiResponse.success("性能统计数据清除成功");
            
        } catch (Exception e) {
            logger.error("清除性能统计数据异常", e);
            return ApiResponse.error("清除性能统计数据失败");
        }
    }

    /**
     * 获取特定计时器的统计信息
     * 
     * @param timerName 计时器名称
     * @return 统计信息
     */
    @GetMapping("/performance/{timerName}")
    public ApiResponse<Map<String, Object>> getTimerStats(@PathVariable String timerName) {
        logger.info("获取计时器统计信息: timerName={}", timerName);

        try {
            PerformanceMonitor.PerformanceStats stats = PerformanceMonitor.getStats(timerName);
            
            if (stats == null) {
                return ApiResponse.notFound("计时器不存在: " + timerName);
            }
            
            Map<String, Object> statMap = new HashMap<>();
            statMap.put("timerName", timerName);
            statMap.put("totalCount", stats.getTotalCount());
            statMap.put("totalTime", stats.getTotalTime());
            statMap.put("averageTime", stats.getAverageTime());
            statMap.put("minTime", stats.getMinTime());
            statMap.put("maxTime", stats.getMaxTime());
            statMap.put("errorCount", stats.getErrorCount());
            statMap.put("successRate", stats.getSuccessRate());
            
            return ApiResponse.success("计时器统计获取成功", statMap);
            
        } catch (Exception e) {
            logger.error("获取计时器统计异常: timerName={}", timerName, e);
            return ApiResponse.error("获取计时器统计失败");
        }
    }

    /**
     * 获取系统信息
     * 
     * @return 系统信息
     */
    @GetMapping("/system")
    public ApiResponse<Map<String, Object>> getSystemInfo() {
        logger.info("获取系统信息");

        try {
            Map<String, Object> systemInfo = new HashMap<>();
            
            // JVM信息
            Runtime runtime = Runtime.getRuntime();
            systemInfo.put("jvmTotalMemory", runtime.totalMemory());
            systemInfo.put("jvmFreeMemory", runtime.freeMemory());
            systemInfo.put("jvmUsedMemory", runtime.totalMemory() - runtime.freeMemory());
            systemInfo.put("jvmMaxMemory", runtime.maxMemory());
            systemInfo.put("availableProcessors", runtime.availableProcessors());
            
            // 系统属性
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("osVersion", System.getProperty("os.version"));
            systemInfo.put("osArch", System.getProperty("os.arch"));
            
            // 应用信息
            systemInfo.put("applicationName", "MLS Upload Server");
            systemInfo.put("version", "1.0.0");
            systemInfo.put("startTime", System.currentTimeMillis());
            
            return ApiResponse.success("系统信息获取成功", systemInfo);
            
        } catch (Exception e) {
            logger.error("获取系统信息异常", e);
            return ApiResponse.error("获取系统信息失败");
        }
    }
}
