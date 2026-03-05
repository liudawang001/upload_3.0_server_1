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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 缓存刷新服务测试类
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheRefreshServiceTest {

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

        // 注入mock的RestTemplate
        ReflectionTestUtils.setField(cacheRefreshService, "restTemplate", restTemplate);
    }

    @Test
    public void testRefreshSingleFileCache_Success() throws Exception {
        // 准备测试数据
        String filename = "test-image.jpg";
        String expectedUrl = "http://localhost:8080/api/cache/update/single?filename=" + filename;
        
        // 模拟成功响应
        ResponseEntity<String> successResponse = new ResponseEntity<>(
            "{\"success\":true,\"message\":\"缓存更新成功\"}", 
            HttpStatus.OK
        );
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenReturn(successResponse);

        // 初始化服务（模拟@PostConstruct）
        cacheRefreshService.init();

        // 执行测试
        cacheRefreshService.refreshSingleFileCache(filename);

        // 等待异步任务完成
        Thread.sleep(1000);

        // 验证RestTemplate被调用
        verify(restTemplate, timeout(2000).times(1))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));

        // 验证统计信息
        String statistics = cacheRefreshService.getStatistics();
        assertTrue("统计信息应包含成功计数", statistics.contains("成功: 1"));
    }

    @Test
    public void testRefreshSingleFileCache_Disabled() throws Exception {
        // 配置服务为禁用状态
        when(properties.isEnabled()).thenReturn(false);

        // 执行测试
        cacheRefreshService.refreshSingleFileCache("test-image.jpg");

        // 等待可能的异步任务
        Thread.sleep(500);

        // 验证RestTemplate未被调用
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    public void testRefreshSingleFileCache_EmptyFilename() throws Exception {
        // 初始化服务
        cacheRefreshService.init();

        // 测试空文件名
        cacheRefreshService.refreshSingleFileCache("");
        cacheRefreshService.refreshSingleFileCache(null);
        cacheRefreshService.refreshSingleFileCache("   ");

        // 等待可能的异步任务
        Thread.sleep(500);

        // 验证RestTemplate未被调用
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    public void testRefreshBatchFilesCache_Success() throws Exception {
        // 准备测试数据
        List<String> filenames = Arrays.asList("image1.jpg", "image2.jpg", "image3.jpg");
        String expectedUrl = "http://localhost:8080/api/cache/update/batch";
        
        // 模拟成功响应
        ResponseEntity<String> successResponse = new ResponseEntity<>(
            "{\"success\":true,\"totalRecords\":3}", 
            HttpStatus.OK
        );
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenReturn(successResponse);

        // 初始化服务
        cacheRefreshService.init();

        // 执行测试
        cacheRefreshService.refreshBatchFilesCache(filenames);

        // 等待异步任务完成
        Thread.sleep(1000);

        // 验证RestTemplate被调用
        verify(restTemplate, timeout(2000).times(1))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));
    }

    @Test
    public void testRefreshBatchFilesCache_EmptyList() throws Exception {
        // 初始化服务
        cacheRefreshService.init();

        // 测试空列表
        cacheRefreshService.refreshBatchFilesCache(Arrays.asList());
        cacheRefreshService.refreshBatchFilesCache(null);

        // 等待可能的异步任务
        Thread.sleep(500);

        // 验证RestTemplate未被调用
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    public void testSmartIncrementalRefresh_Success() throws Exception {
        // 准备测试数据
        String expectedUrl = "http://localhost:8080/api/cache/update/incremental";
        
        // 模拟成功响应
        ResponseEntity<String> successResponse = new ResponseEntity<>(
            "{\"success\":true,\"updateType\":\"INCREMENTAL\"}", 
            HttpStatus.OK
        );
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenReturn(successResponse);

        // 初始化服务
        cacheRefreshService.init();

        // 执行测试
        cacheRefreshService.smartIncrementalRefresh();

        // 等待异步任务完成
        Thread.sleep(1000);

        // 验证RestTemplate被调用
        verify(restTemplate, timeout(2000).times(1))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));
    }

    @Test
    public void testGetStatistics() {
        // 初始化服务
        cacheRefreshService.init();

        // 获取统计信息
        String statistics = cacheRefreshService.getStatistics();

        // 验证统计信息格式
        assertNotNull("统计信息不应为null", statistics);
        assertTrue("统计信息应包含成功计数", statistics.contains("成功:"));
        assertTrue("统计信息应包含失败计数", statistics.contains("失败:"));
        assertTrue("统计信息应包含队列大小", statistics.contains("队列大小:"));
        assertTrue("统计信息应包含活跃线程", statistics.contains("活跃线程:"));
    }

    @Test
    public void testRetryMechanism() throws Exception {
        // 准备测试数据
        String filename = "test-retry.jpg";
        String expectedUrl = "http://localhost:8080/api/cache/update/single?filename=" + filename;
        
        // 模拟前两次失败，第三次成功
        when(restTemplate.postForEntity(eq(expectedUrl), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Network error"))
            .thenThrow(new RuntimeException("Network error"))
            .thenReturn(new ResponseEntity<>("{\"success\":true}", HttpStatus.OK));

        // 初始化服务
        cacheRefreshService.init();

        // 执行测试
        cacheRefreshService.refreshSingleFileCache(filename);

        // 等待重试完成（考虑指数退避延迟）
        Thread.sleep(5000);

        // 验证RestTemplate被调用3次（2次失败 + 1次成功）
        verify(restTemplate, times(3))
            .postForEntity(eq(expectedUrl), any(), eq(String.class));
    }

    @Test
    public void testServiceDestroy() {
        // 初始化服务
        cacheRefreshService.init();

        // 获取线程池引用
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ReflectionTestUtils.getField(cacheRefreshService, "cacheRefreshExecutor");
        assertNotNull("线程池应已初始化", executor);
        assertFalse("线程池应处于运行状态", executor.isShutdown());

        // 销毁服务
        cacheRefreshService.destroy();

        // 验证线程池已关闭
        assertTrue("线程池应已关闭", executor.isShutdown());
    }
}
