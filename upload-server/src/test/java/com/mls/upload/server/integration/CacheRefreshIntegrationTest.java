package com.mls.upload.server.integration;

import com.mls.upload.server.config.CacheRefreshProperties;
import com.mls.upload.server.service.CacheRefreshService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 缓存刷新集成测试
 * 测试缓存刷新服务的完整配置和功能
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "cache.refresh.enabled=true",
    "cache.refresh.target-url=http://localhost:8080",
    "cache.refresh.timeout.connect=5000",
    "cache.refresh.timeout.read=10000",
    "cache.refresh.retry.max-attempts=3",
    "cache.refresh.retry.initial-delay=1000",
    "cache.refresh.thread-pool.core-size=2",
    "cache.refresh.thread-pool.max-size=5",
    "cache.refresh.thread-pool.queue-capacity=100",
    "cache.refresh.batch.threshold=5",
    "cache.refresh.batch.collect-timeout=2000"
})
public class CacheRefreshIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheRefreshIntegrationTest.class);

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Autowired
    private CacheRefreshProperties cacheRefreshProperties;

    @Test
    public void testCacheRefreshPropertiesConfiguration() {
        logger.info("测试缓存刷新配置属性");

        // 验证配置属性正确加载
        assertTrue("缓存刷新功能应启用", cacheRefreshProperties.isEnabled());
        assertEquals("目标URL应正确配置", "http://localhost:8080", cacheRefreshProperties.getTargetUrl());
        assertEquals("连接超时应正确配置", 5000, cacheRefreshProperties.getTimeout().getConnect());
        assertEquals("读取超时应正确配置", 10000, cacheRefreshProperties.getTimeout().getRead());
        assertEquals("最大重试次数应正确配置", 3, cacheRefreshProperties.getRetry().getMaxAttempts());
        assertEquals("初始延迟应正确配置", 1000L, cacheRefreshProperties.getRetry().getInitialDelay());
        assertEquals("核心线程数应正确配置", 2, cacheRefreshProperties.getThreadPool().getCoreSize());
        assertEquals("最大线程数应正确配置", 5, cacheRefreshProperties.getThreadPool().getMaxSize());
        assertEquals("队列容量应正确配置", 100, cacheRefreshProperties.getThreadPool().getQueueCapacity());
        assertEquals("批量阈值应正确配置", 5, cacheRefreshProperties.getBatch().getThreshold());
        assertEquals("批量收集超时应正确配置", 2000L, cacheRefreshProperties.getBatch().getCollectTimeout());

        logger.info("缓存刷新配置属性测试通过");
    }

    @Test
    public void testCacheRefreshServiceInitialization() {
        logger.info("测试缓存刷新服务初始化");

        // 验证服务正确注入
        assertNotNull("缓存刷新服务应正确注入", cacheRefreshService);

        // 获取统计信息验证服务状态
        String statistics = cacheRefreshService.getStatistics();
        assertNotNull("统计信息不应为null", statistics);
        assertTrue("统计信息应包含成功计数", statistics.contains("成功:"));
        assertTrue("统计信息应包含失败计数", statistics.contains("失败:"));

        logger.info("缓存刷新服务初始化测试通过");
        logger.info("当前统计信息: {}", statistics);
    }

    @Test
    public void testSingleFileCacheRefreshCall() {
        logger.info("测试单文件缓存刷新调用");

        String testFilename = "integration-test-image.jpg";

        // 调用单文件缓存刷新（异步）
        cacheRefreshService.refreshSingleFileCache(testFilename);

        // 等待异步任务可能的执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证调用不会抛出异常
        String statistics = cacheRefreshService.getStatistics();
        logger.info("单文件缓存刷新后统计信息: {}", statistics);

        logger.info("单文件缓存刷新调用测试通过");
    }

    @Test
    public void testBatchFilesCacheRefreshCall() {
        logger.info("测试批量文件缓存刷新调用");

        List<String> testFilenames = Arrays.asList(
            "integration-test-1.jpg",
            "integration-test-2.jpg",
            "integration-test-3.jpg"
        );

        // 调用批量文件缓存刷新（异步）
        cacheRefreshService.refreshBatchFilesCache(testFilenames);

        // 等待异步任务可能的执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证调用不会抛出异常
        String statistics = cacheRefreshService.getStatistics();
        logger.info("批量文件缓存刷新后统计信息: {}", statistics);

        logger.info("批量文件缓存刷新调用测试通过");
    }

    @Test
    public void testSmartIncrementalRefreshCall() {
        logger.info("测试智能增量缓存刷新调用");

        // 调用智能增量缓存刷新（异步）
        cacheRefreshService.smartIncrementalRefresh();

        // 等待异步任务可能的执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证调用不会抛出异常
        String statistics = cacheRefreshService.getStatistics();
        logger.info("智能增量缓存刷新后统计信息: {}", statistics);

        logger.info("智能增量缓存刷新调用测试通过");
    }

    @Test
    public void testInvalidInputHandling() {
        logger.info("测试无效输入处理");

        // 测试空文件名
        cacheRefreshService.refreshSingleFileCache(null);
        cacheRefreshService.refreshSingleFileCache("");
        cacheRefreshService.refreshSingleFileCache("   ");

        // 测试空文件列表
        cacheRefreshService.refreshBatchFilesCache(null);
        cacheRefreshService.refreshBatchFilesCache(Arrays.asList());

        // 等待可能的异步任务
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证服务仍然正常工作
        String statistics = cacheRefreshService.getStatistics();
        assertNotNull("统计信息应仍然可用", statistics);

        logger.info("无效输入处理测试通过");
    }

    @Test
    public void testConfigurationToString() {
        logger.info("测试配置对象toString方法");

        String configString = cacheRefreshProperties.toString();
        assertNotNull("配置字符串不应为null", configString);
        assertTrue("配置字符串应包含enabled", configString.contains("enabled"));
        assertTrue("配置字符串应包含targetUrl", configString.contains("targetUrl"));

        logger.info("配置对象toString: {}", configString);
        logger.info("配置对象toString测试通过");
    }
}
