package com.mls.upload.server.service;

import com.mls.upload.server.config.CacheRefreshProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 降级处理功能测试
 */
@RunWith(MockitoJUnitRunner.class)
public class FallbackCacheRefreshTest {

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
        lenient().when(batchConfig.getMaxConcurrentBatches()).thenReturn(3);
        when(batchConfig.getMaxSingleBatchSize()).thenReturn(3000);
    }

    @Test
    public void testMegaBatchAsyncEnabled() {
        // 测试启用超大批量异步API
        when(batchConfig.isEnableMegaBatchAsync()).thenReturn(true);
        
        List<String> megaBatch = createFileList(2500);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(megaBatch);
        
        assertEquals("应该选择MEGA_BATCH_ASYNC API", ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection.getApiType());
        assertTrue("应该是异步处理", selection.isAsync());
        assertTrue("原因应该包含超大批量异步处理", selection.getReason().contains("超大批量异步处理"));
    }

    @Test
    public void testMegaBatchAsyncDisabled() {
        // 测试禁用超大批量异步API时的降级
        when(batchConfig.isEnableMegaBatchAsync()).thenReturn(false);
        
        List<String> megaBatch = createFileList(2500);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(megaBatch);
        
        assertEquals("应该降级到LARGE_BATCH API", ApiSelector.ApiType.LARGE_BATCH, selection.getApiType());
        assertFalse("应该不是异步处理", selection.isAsync());
        assertTrue("原因应该包含降级信息", selection.getReason().contains("降级到大批量处理"));
        assertTrue("原因应该包含异步API已禁用", selection.getReason().contains("异步API已禁用"));
    }

    @Test
    public void testForcedBatchingCheck() {
        // 测试强制分批检查
        
        // 不需要强制分批
        assertFalse("2000个文件不需要强制分批", apiSelector.needsForcedBatching(2000));
        assertFalse("3000个文件不需要强制分批", apiSelector.needsForcedBatching(3000));
        
        // 需要强制分批
        assertTrue("3001个文件需要强制分批", apiSelector.needsForcedBatching(3001));
        assertTrue("5000个文件需要强制分批", apiSelector.needsForcedBatching(5000));
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值处理
        when(batchConfig.isEnableMegaBatchAsync()).thenReturn(true);
        
        // 刚好达到超大批量阈值
        List<String> atThreshold = createFileList(2000);
        ApiSelector.ApiSelection selection1 = apiSelector.selectOptimalApi(atThreshold);
        assertEquals("刚好2000个文件应该选择MEGA_BATCH_ASYNC", ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection1.getApiType());
        
        // 刚好低于超大批量阈值
        List<String> belowThreshold = createFileList(1999);
        ApiSelector.ApiSelection selection2 = apiSelector.selectOptimalApi(belowThreshold);
        assertEquals("1999个文件应该选择LARGE_BATCH", ApiSelector.ApiType.LARGE_BATCH, selection2.getApiType());
    }

    @Test
    public void testConfigurationValidation() {
        // 测试配置验证
        lenient().when(batchConfig.isEnableMegaBatchAsync()).thenReturn(true);
        assertTrue("配置应该有效", apiSelector.validateConfiguration());
        
        // 测试无效配置 - 阈值为0
        lenient().when(batchConfig.getThreshold()).thenReturn(0);
        assertFalse("阈值为0时配置应该无效", apiSelector.validateConfiguration());
        
        // 恢复有效配置
        lenient().when(batchConfig.getThreshold()).thenReturn(5);
        assertTrue("恢复后配置应该有效", apiSelector.validateConfiguration());
    }

    @Test
    public void testApiSelectionWithFallbackDisabled() {
        // 测试禁用降级处理时的行为
        lenient().when(batchConfig.isEnableMegaBatchAsync()).thenReturn(false);
        
        // 即使禁用了异步API，仍然应该能够选择合适的API
        List<String> largeBatch = createFileList(800);
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(largeBatch);
        
        assertEquals("应该选择LARGE_BATCH API", ApiSelector.ApiType.LARGE_BATCH, selection.getApiType());
        assertFalse("应该不是异步处理", selection.isAsync());
    }

    @Test
    public void testSelectionStatsWithFallback() {
        // 测试降级情况下的统计信息
        when(batchConfig.isEnableMegaBatchAsync()).thenReturn(false);
        
        String stats = apiSelector.getSelectionStats(2500);
        assertTrue("统计信息应该包含文件数量", stats.contains("文件数量=2500"));
        assertTrue("统计信息应该包含large-batch", stats.contains("选择API=large-batch"));
        assertTrue("统计信息应该显示非异步", stats.contains("异步=false"));
    }

    @Test
    public void testOptimalBatchSizeCalculation() {
        // 测试最优批次大小计算
        
        // 小批量
        assertEquals("500个文件的最优批次大小", 500, apiSelector.calculateOptimalBatchSize(500));
        
        // 中等批量
        assertEquals("1500个文件的最优批次大小", 1000, apiSelector.calculateOptimalBatchSize(1500));
        
        // 大批量
        assertEquals("3000个文件的最优批次大小", 1000, apiSelector.calculateOptimalBatchSize(3000));
        
        // 超大批量
        assertEquals("5000个文件的最优批次大小", 1666, apiSelector.calculateOptimalBatchSize(5000));
    }

    @Test
    public void testNeedsBatchingVsNeedsForcedBatching() {
        // 测试普通分批vs强制分批的区别
        
        // 普通分批检查（基于maxBatchSize=5000）
        assertFalse("5000个文件不需要普通分批", apiSelector.needsBatching(5000));
        assertTrue("5001个文件需要普通分批", apiSelector.needsBatching(5001));
        
        // 强制分批检查（基于maxSingleBatchSize=3000）
        assertFalse("3000个文件不需要强制分批", apiSelector.needsForcedBatching(3000));
        assertTrue("3001个文件需要强制分批", apiSelector.needsForcedBatching(3001));
        
        // 验证强制分批阈值更严格
        assertTrue("强制分批阈值应该更严格", 
                  apiSelector.needsForcedBatching(4000) && !apiSelector.needsBatching(4000));
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
