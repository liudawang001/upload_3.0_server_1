package com.mls.upload.server.service;

import com.mls.upload.server.config.CacheRefreshProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 延迟缓存刷新功能测试
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DelayedCacheRefreshTest {

    @Mock
    private CacheRefreshProperties properties;

    @Mock
    private CacheRefreshProperties.Timeout timeout;

    @Mock
    private CacheRefreshProperties.Retry retry;

    @Mock
    private CacheRefreshProperties.ThreadPool threadPool;

    @Mock
    private CacheRefreshProperties.Batch batch;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CacheRefreshService cacheRefreshService;

    @Before
    public void setUp() {
        // 配置基本的mock行为
        lenient().when(properties.isEnabled()).thenReturn(true);
        lenient().when(properties.getTargetUrl()).thenReturn("http://localhost:8080");
        lenient().when(properties.getTimeout()).thenReturn(timeout);
        lenient().when(properties.getRetry()).thenReturn(retry);
        lenient().when(properties.getThreadPool()).thenReturn(threadPool);
        lenient().when(properties.getBatch()).thenReturn(batch);

        lenient().when(timeout.getConnect()).thenReturn(5000);
        lenient().when(timeout.getRead()).thenReturn(10000);

        lenient().when(retry.getMaxAttempts()).thenReturn(3);
        lenient().when(retry.getInitialDelay()).thenReturn(1000L);

        lenient().when(threadPool.getCoreSize()).thenReturn(2);
        lenient().when(threadPool.getMaxSize()).thenReturn(5);
        lenient().when(threadPool.getQueueCapacity()).thenReturn(100);

        lenient().when(batch.getThreshold()).thenReturn(5);
        lenient().when(batch.getCollectTimeout()).thenReturn(2000L);
        lenient().when(batch.getMaxWaitTimeout()).thenReturn(30000L);

        // 注入mock的RestTemplate
        ReflectionTestUtils.setField(cacheRefreshService, "restTemplate", restTemplate);
    }

    @Test
    public void testCollectSuccessfulFile() {
        // 初始化服务
        cacheRefreshService.init();

        // 测试收集文件
        cacheRefreshService.collectSuccessfulFile("test1.jpg");
        cacheRefreshService.collectSuccessfulFile("test2.jpg");
        cacheRefreshService.collectSuccessfulFile("test1.jpg"); // 重复文件

        // 获取内部队列进行验证
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> queue = (ConcurrentLinkedQueue<String>) 
            ReflectionTestUtils.getField(cacheRefreshService, "successfulFilesQueue");

        assertNotNull("成功文件队列应该存在", queue);
        assertEquals("队列中应该有2个不重复的文件", 2, queue.size());
        assertTrue("队列应包含test1.jpg", queue.contains("test1.jpg"));
        assertTrue("队列应包含test2.jpg", queue.contains("test2.jpg"));
    }

    @Test
    public void testCollectSuccessfulFile_EmptyFilename() {
        // 初始化服务
        cacheRefreshService.init();

        // 测试空文件名
        cacheRefreshService.collectSuccessfulFile(null);
        cacheRefreshService.collectSuccessfulFile("");
        cacheRefreshService.collectSuccessfulFile("   ");

        // 获取内部队列进行验证
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> queue = (ConcurrentLinkedQueue<String>) 
            ReflectionTestUtils.getField(cacheRefreshService, "successfulFilesQueue");

        assertNotNull("成功文件队列应该存在", queue);
        assertEquals("队列应该为空", 0, queue.size());
    }

    @Test
    public void testTriggerBatchRefreshIfReady_SingleFile() throws Exception {
        // 准备测试数据
        String expectedUrl = "http://localhost:8080/api/cache/update/single?filename=test.jpg";
        
        // 模拟成功响应
        ResponseEntity<String> successResponse = new ResponseEntity<>(
            "{\"success\":true}", HttpStatus.OK
        );
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenReturn(successResponse);

        // 初始化服务
        cacheRefreshService.init();

        // 收集一个文件
        cacheRefreshService.collectSuccessfulFile("test.jpg");

        // 触发批量刷新
        cacheRefreshService.triggerBatchRefreshIfReady();

        // 等待异步任务完成
        Thread.sleep(1000);

        // 验证调用了单文件API
        verify(restTemplate, timeout(2000).times(1))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));

        // 验证队列已清空
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> queue = (ConcurrentLinkedQueue<String>) 
            ReflectionTestUtils.getField(cacheRefreshService, "successfulFilesQueue");
        assertEquals("队列应该已清空", 0, queue.size());
    }

    @Test
    public void testTriggerBatchRefreshIfReady_MultipleFiles() throws Exception {
        // 准备测试数据
        String expectedUrl = "http://localhost:8080/api/cache/update/batch";
        
        // 模拟成功响应
        ResponseEntity<String> successResponse = new ResponseEntity<>(
            "{\"success\":true,\"totalRecords\":3}", HttpStatus.OK
        );
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenReturn(successResponse);

        // 初始化服务
        cacheRefreshService.init();

        // 收集多个文件
        cacheRefreshService.collectSuccessfulFile("test1.jpg");
        cacheRefreshService.collectSuccessfulFile("test2.jpg");
        cacheRefreshService.collectSuccessfulFile("test3.jpg");

        // 触发批量刷新
        cacheRefreshService.triggerBatchRefreshIfReady();

        // 等待异步任务完成
        Thread.sleep(1000);

        // 验证调用了批量API
        verify(restTemplate, timeout(2000).times(1))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));

        // 验证队列已清空
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> queue = (ConcurrentLinkedQueue<String>) 
            ReflectionTestUtils.getField(cacheRefreshService, "successfulFilesQueue");
        assertEquals("队列应该已清空", 0, queue.size());
    }

    @Test
    public void testTriggerBatchRefreshIfReady_EmptyQueue() {
        // 初始化服务
        cacheRefreshService.init();

        // 不收集任何文件，直接触发
        cacheRefreshService.triggerBatchRefreshIfReady();

        // 验证没有调用任何API
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    public void testTriggerBatchRefreshIfReady_Disabled() {
        // 配置服务为禁用状态
        when(properties.isEnabled()).thenReturn(false);

        // 收集文件
        cacheRefreshService.collectSuccessfulFile("test.jpg");

        // 触发批量刷新
        cacheRefreshService.triggerBatchRefreshIfReady();

        // 验证没有调用任何API
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    public void testGetStatistics_WithSuccessfulFiles() {
        // 初始化服务
        cacheRefreshService.init();

        // 收集一些文件
        cacheRefreshService.collectSuccessfulFile("test1.jpg");
        cacheRefreshService.collectSuccessfulFile("test2.jpg");

        // 获取统计信息
        String statistics = cacheRefreshService.getStatistics();

        // 验证统计信息包含待刷新文件数量
        assertNotNull("统计信息不应为null", statistics);
        assertTrue("统计信息应包含待刷新文件数量", statistics.contains("待刷新文件: 2"));
    }
}
