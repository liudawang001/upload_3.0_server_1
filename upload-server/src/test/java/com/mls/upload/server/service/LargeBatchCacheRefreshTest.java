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
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.junit.Rule;

/**
 * 大批量缓存刷新功能测试
 */
@RunWith(MockitoJUnitRunner.class)
public class LargeBatchCacheRefreshTest {

    @Mock
    private CacheRefreshProperties properties;

    @Mock
    private CacheRefreshProperties.Batch batchConfig;

    @InjectMocks
    private ApiSelector apiSelector;

    @Before
    public void setUp() {
        // 配置mock对象
        when(properties.getBatch()).thenReturn(batchConfig);
        when(properties.getTargetUrl()).thenReturn("http://localhost:8080");
        
        // 配置批量处理阈值
        when(batchConfig.getThreshold()).thenReturn(5);
        when(batchConfig.getLargeBatchThreshold()).thenReturn(500);
        when(batchConfig.getMegaBatchThreshold()).thenReturn(2000);
        when(batchConfig.getMaxBatchSize()).thenReturn(5000);
        lenient().when(batchConfig.getDefaultBatchSize()).thenReturn(500);
        when(batchConfig.getOptimalBatchSize()).thenReturn(1000);
        when(batchConfig.getMaxConcurrentBatches()).thenReturn(3);
    }

    @Test
    public void testApiSelectorSingleFile() {
        // 测试单文件API选择
        List<String> singleFile = Arrays.asList("test1.jpg");
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(singleFile);
        
        assertEquals(ApiSelector.ApiType.SINGLE, selection.getApiType());
        assertEquals("http://localhost:8080/api/cache/update/single", selection.getEndpoint());
        assertFalse(selection.isAsync());
        assertEquals("单文件处理", selection.getReason());
    }

    @Test
    public void testApiSelectorSmallBatch() {
        // 测试小批量API选择
        List<String> smallBatch = createFileList(100);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(smallBatch);

        assertEquals(ApiSelector.ApiType.BATCH, selection.getApiType());
        assertEquals("http://localhost:8080/api/cache/update/batch", selection.getEndpoint());
        assertFalse(selection.isAsync());
        assertTrue(selection.getReason().contains("小批量处理"));
    }

    @Test
    public void testApiSelectorLargeBatch() {
        // 测试大批量API选择
        List<String> largeBatch = createFileList(800);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(largeBatch);

        assertEquals(ApiSelector.ApiType.LARGE_BATCH, selection.getApiType());
        assertEquals("http://localhost:8080/api/cache/update/large-batch", selection.getEndpoint());
        assertFalse(selection.isAsync());
        assertTrue(selection.getReason().contains("大批量处理"));
    }

    @Test
    public void testApiSelectorMegaBatch() {
        // 测试超大批量API选择
        List<String> megaBatch = createFileList(3000);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(megaBatch);

        assertEquals(ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection.getApiType());
        assertEquals("http://localhost:8080/api/cache/update/large-batch-async", selection.getEndpoint());
        assertTrue(selection.isAsync());
        assertTrue(selection.getReason().contains("超大批量异步处理"));
    }

    @Test
    public void testApiSelectorBoundaryValues() {
        // 测试边界值
        
        // 刚好达到大批量阈值
        List<String> atLargeBatchThreshold = createFileList(500);
        ApiSelector.ApiSelection selection1 = apiSelector.selectOptimalApi(atLargeBatchThreshold);
        assertEquals(ApiSelector.ApiType.LARGE_BATCH, selection1.getApiType());
        
        // 刚好低于大批量阈值
        List<String> belowLargeBatchThreshold = createFileList(499);
        ApiSelector.ApiSelection selection2 = apiSelector.selectOptimalApi(belowLargeBatchThreshold);
        assertEquals(ApiSelector.ApiType.BATCH, selection2.getApiType());
        
        // 刚好达到超大批量阈值
        List<String> atMegaBatchThreshold = createFileList(2000);
        ApiSelector.ApiSelection selection3 = apiSelector.selectOptimalApi(atMegaBatchThreshold);
        assertEquals(ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection3.getApiType());
        
        // 刚好低于超大批量阈值
        List<String> belowMegaBatchThreshold = createFileList(1999);
        ApiSelector.ApiSelection selection4 = apiSelector.selectOptimalApi(belowMegaBatchThreshold);
        assertEquals(ApiSelector.ApiType.LARGE_BATCH, selection4.getApiType());
    }

    @Test
    public void testCalculateOptimalBatchSize() {
        // 测试最优批次大小计算
        
        // 小于最优批次大小
        assertEquals(500, apiSelector.calculateOptimalBatchSize(500));
        
        // 中等大小
        assertEquals(1000, apiSelector.calculateOptimalBatchSize(1500));
        
        // 大批量
        assertEquals(1000, apiSelector.calculateOptimalBatchSize(2500));
        
        // 超大批量
        assertEquals(1666, apiSelector.calculateOptimalBatchSize(5000));
    }

    @Test
    public void testNeedsBatching() {
        // 测试是否需要分批处理
        assertFalse(apiSelector.needsBatching(1000));
        assertFalse(apiSelector.needsBatching(5000));
        assertTrue(apiSelector.needsBatching(5001));
    }

    @Test
    public void testValidateConfiguration() {
        // 测试配置验证
        assertTrue(apiSelector.validateConfiguration());
        
        // 测试无效配置
        when(batchConfig.getThreshold()).thenReturn(0);
        assertFalse(apiSelector.validateConfiguration());
        
        // 恢复有效配置
        when(batchConfig.getThreshold()).thenReturn(5);
        when(batchConfig.getLargeBatchThreshold()).thenReturn(3); // 小于threshold
        assertFalse(apiSelector.validateConfiguration());
    }

    @Test
    public void testGetSelectionStats() {
        // 测试统计信息
        String stats = apiSelector.getSelectionStats(800);
        assertTrue(stats.contains("文件数量=800"));
        assertTrue(stats.contains("选择API=large-batch"));
        assertTrue(stats.contains("异步=false"));
    }

    @Test
    public void testApiSelectionToString() {
        // 测试ApiSelection的toString方法
        List<String> files = createFileList(100);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(files);
        String toString = selection.toString();
        
        assertTrue(toString.contains("ApiSelection"));
        assertTrue(toString.contains("type=batch"));
        assertTrue(toString.contains("endpoint="));
        assertTrue(toString.contains("async=false"));
    }

    @Test
    public void testInvalidInputs() {
        // 测试无效输入
        assertThrows(IllegalArgumentException.class, () -> apiSelector.selectOptimalApi((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> apiSelector.selectOptimalApi(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> apiSelector.selectOptimalApi(0));
        assertThrows(IllegalArgumentException.class, () -> apiSelector.selectOptimalApi(-1));
    }

    /**
     * 创建指定数量的文件列表
     */
    private List<String> createFileList(int count) {
        List<String> files = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            files.add(String.format("test%04d.jpg", i));
        }
        return files;
    }
}
