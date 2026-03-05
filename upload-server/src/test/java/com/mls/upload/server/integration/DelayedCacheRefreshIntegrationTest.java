package com.mls.upload.server.integration;

import com.mls.upload.server.config.CacheRefreshProperties;
import com.mls.upload.server.service.CacheRefreshService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;

/**
 * 延迟缓存刷新集成测试
 * 验证端到端的延迟调用功能
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
    "cache.refresh.batch.collect-timeout=2000",
    "cache.refresh.batch.max-wait-timeout=30000"
})
public class DelayedCacheRefreshIntegrationTest {

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Autowired
    private CacheRefreshProperties properties;

    @After
    public void tearDown() {
        // 清理测试后的状态，确保队列为空
        try {
            cacheRefreshService.triggerBatchRefreshIfReady();
            Thread.sleep(100); // 等待清理完成
        } catch (Exception e) {
            // 忽略清理过程中的异常
        }
    }

    @Test
    public void testConfigurationLoading() {
        // 验证配置正确加载
        assertNotNull("CacheRefreshService应该被正确注入", cacheRefreshService);
        assertNotNull("CacheRefreshProperties应该被正确注入", properties);
        
        assertTrue("缓存刷新功能应该启用", properties.isEnabled());
        assertEquals("目标URL应该正确", "http://localhost:8080", properties.getTargetUrl());
        assertEquals("连接超时应该正确", 5000, properties.getTimeout().getConnect());
        assertEquals("读取超时应该正确", 10000, properties.getTimeout().getRead());
        assertEquals("最大重试次数应该正确", 3, properties.getRetry().getMaxAttempts());
        assertEquals("初始延迟应该正确", 1000L, properties.getRetry().getInitialDelay());
        assertEquals("核心线程数应该正确", 2, properties.getThreadPool().getCoreSize());
        assertEquals("最大线程数应该正确", 5, properties.getThreadPool().getMaxSize());
        assertEquals("队列容量应该正确", 100, properties.getThreadPool().getQueueCapacity());
        assertEquals("批量阈值应该正确", 5, properties.getBatch().getThreshold());
        assertEquals("收集超时应该正确", 2000L, properties.getBatch().getCollectTimeout());
        assertEquals("最大等待超时应该正确", 30000L, properties.getBatch().getMaxWaitTimeout());
    }

    @Test
    public void testServiceInitialization() {
        // 验证服务正确初始化
        String statistics = cacheRefreshService.getStatistics();
        assertNotNull("统计信息不应为null", statistics);
        assertTrue("统计信息应包含成功计数", statistics.contains("成功:"));
        assertTrue("统计信息应包含失败计数", statistics.contains("失败:"));
        assertTrue("统计信息应包含队列大小", statistics.contains("队列大小:"));
        assertTrue("统计信息应包含待刷新文件", statistics.contains("待刷新文件:"));
        assertTrue("统计信息应包含活跃线程", statistics.contains("活跃线程:"));
    }

    @Test
    public void testCollectSuccessfulFiles() throws Exception {
        // 测试文件收集功能
        cacheRefreshService.collectSuccessfulFile("test1.jpg");
        cacheRefreshService.collectSuccessfulFile("test2.jpg");
        cacheRefreshService.collectSuccessfulFile("test3.jpg");
        
        // 等待一小段时间确保收集完成
        Thread.sleep(100);
        
        String statistics = cacheRefreshService.getStatistics();
        assertTrue("统计信息应显示有待刷新文件", statistics.contains("待刷新文件: 3"));
    }

    @Test
    public void testCollectDuplicateFiles() throws Exception {
        // 测试重复文件收集
        cacheRefreshService.collectSuccessfulFile("duplicate.jpg");
        cacheRefreshService.collectSuccessfulFile("duplicate.jpg");
        cacheRefreshService.collectSuccessfulFile("unique.jpg");
        
        // 等待一小段时间确保收集完成
        Thread.sleep(100);
        
        String statistics = cacheRefreshService.getStatistics();
        assertTrue("统计信息应显示有2个待刷新文件（去重后）", statistics.contains("待刷新文件: 2"));
    }

    @Test
    public void testCollectEmptyFilenames() throws Exception {
        // 测试空文件名处理
        cacheRefreshService.collectSuccessfulFile(null);
        cacheRefreshService.collectSuccessfulFile("");
        cacheRefreshService.collectSuccessfulFile("   ");
        cacheRefreshService.collectSuccessfulFile("valid.jpg");
        
        // 等待一小段时间确保收集完成
        Thread.sleep(100);
        
        String statistics = cacheRefreshService.getStatistics();
        assertTrue("统计信息应显示有1个待刷新文件（只有有效文件）", statistics.contains("待刷新文件: 1"));
    }

    @Test
    public void testTriggerBatchRefreshWithEmptyQueue() {
        // 测试空队列时的批量刷新触发
        cacheRefreshService.triggerBatchRefreshIfReady();
        
        // 验证没有异常抛出，功能正常
        String statistics = cacheRefreshService.getStatistics();
        assertNotNull("统计信息应该正常返回", statistics);
    }

    @Test
    public void testTriggerBatchRefreshWithFiles() throws Exception {
        // 收集一些文件
        cacheRefreshService.collectSuccessfulFile("batch1.jpg");
        cacheRefreshService.collectSuccessfulFile("batch2.jpg");
        cacheRefreshService.collectSuccessfulFile("batch3.jpg");
        
        // 等待收集完成
        Thread.sleep(100);
        
        // 验证文件已收集
        String statisticsBefore = cacheRefreshService.getStatistics();
        assertTrue("触发前应有待刷新文件", statisticsBefore.contains("待刷新文件: 3"));
        
        // 触发批量刷新（注意：由于没有真实的项目B运行，API调用会失败，但不影响测试逻辑）
        cacheRefreshService.triggerBatchRefreshIfReady();
        
        // 等待处理完成
        Thread.sleep(2000);
        
        // 验证队列已清空（无论API调用成功与否，队列都会被清空）
        String statisticsAfter = cacheRefreshService.getStatistics();
        assertTrue("触发后待刷新文件应为0", statisticsAfter.contains("待刷新文件: 0"));
    }

    @Test
    public void testServiceStatisticsFormat() {
        // 测试统计信息格式
        String statistics = cacheRefreshService.getStatistics();
        
        // 验证统计信息格式正确
        assertTrue("统计信息应包含'缓存刷新统计'", statistics.contains("缓存刷新统计"));
        assertTrue("统计信息应包含成功计数", statistics.matches(".*成功: \\d+.*"));
        assertTrue("统计信息应包含失败计数", statistics.matches(".*失败: \\d+.*"));
        assertTrue("统计信息应包含队列大小", statistics.matches(".*队列大小: \\d+.*"));
        assertTrue("统计信息应包含待刷新文件", statistics.matches(".*待刷新文件: \\d+.*"));
        assertTrue("统计信息应包含活跃线程", statistics.matches(".*活跃线程: \\d+.*"));
    }

    @Test
    public void testConcurrentFileCollection() throws Exception {
        // 测试并发文件收集
        int threadCount = 10;
        int filesPerThread = 5;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < filesPerThread; j++) {
                    cacheRefreshService.collectSuccessfulFile(String.format("thread%d_file%d.jpg", threadId, j));
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 等待收集完成
        Thread.sleep(500);
        
        // 验证所有文件都被收集
        String statistics = cacheRefreshService.getStatistics();
        int expectedFiles = threadCount * filesPerThread;
        assertTrue(String.format("应收集到%d个文件", expectedFiles), 
                  statistics.contains(String.format("待刷新文件: %d", expectedFiles)));
    }

    @Test
    public void testServiceShutdownGracefully() throws Exception {
        // 收集一些文件
        cacheRefreshService.collectSuccessfulFile("shutdown1.jpg");
        cacheRefreshService.collectSuccessfulFile("shutdown2.jpg");
        
        // 等待收集完成
        Thread.sleep(100);
        
        // 验证服务状态正常
        String statistics = cacheRefreshService.getStatistics();
        assertTrue("关闭前应有待刷新文件", statistics.contains("待刷新文件: 2"));
        
        // 模拟服务关闭（在实际应用中由Spring容器管理）
        try {
            cacheRefreshService.destroy();
        } catch (Exception e) {
            // 关闭过程中可能有异常，这是正常的
        }
        
        // 验证关闭后服务仍可调用（但功能可能受限）
        assertNotNull("关闭后统计信息仍应可获取", cacheRefreshService.getStatistics());
    }
}
