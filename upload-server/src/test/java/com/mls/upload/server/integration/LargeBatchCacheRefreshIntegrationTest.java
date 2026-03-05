package com.mls.upload.server.integration;

import com.mls.upload.server.config.CacheRefreshProperties;
import com.mls.upload.server.service.ApiSelector;
import com.mls.upload.server.service.CacheRefreshService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 大批量缓存刷新集成测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "cache.refresh.enabled=true",
        "cache.refresh.target-url=http://localhost:8080",
        "cache.refresh.batch.threshold=5",
        "cache.refresh.batch.large-batch-threshold=500",
        "cache.refresh.batch.mega-batch-threshold=2000",
        "cache.refresh.batch.max-batch-size=5000",
        "cache.refresh.batch.default-batch-size=500",
        "cache.refresh.batch.optimal-batch-size=1000",
        "cache.refresh.batch.max-concurrent-batches=3",
        "cache.refresh.batch.enable-parallel-processing=true",
        "cache.refresh.batch.memory-threshold-mb=512",
        "cache.refresh.batch.enable-memory-monitoring=true",
        "cache.refresh.batch.batch-timeout-seconds=600",
        "cache.refresh.batch.total-timeout-seconds=3600",
        "cache.refresh.batch.enable-partial-success=true",
        "cache.refresh.batch.enable-progress-tracking=true",
        "cache.refresh.batch.progress-report-interval=100"
})
public class LargeBatchCacheRefreshIntegrationTest {

    @Autowired
    private CacheRefreshProperties properties;

    @Autowired
    private ApiSelector apiSelector;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Test
    public void testConfigurationLoading() {
        // 测试配置加载
        assertNotNull("CacheRefreshProperties应该被正确注入", properties);
        assertTrue("缓存刷新功能应该启用", properties.isEnabled());
        assertEquals("目标URL应该正确配置", "http://localhost:8080", properties.getTargetUrl());
        
        // 测试批量配置
        CacheRefreshProperties.Batch batchConfig = properties.getBatch();
        assertNotNull("批量配置应该存在", batchConfig);
        assertEquals("批量阈值应该正确", 5, batchConfig.getThreshold());
        assertEquals("大批量阈值应该正确", 500, batchConfig.getLargeBatchThreshold());
        assertEquals("超大批量阈值应该正确", 2000, batchConfig.getMegaBatchThreshold());
        assertEquals("最大批处理大小应该正确", 5000, batchConfig.getMaxBatchSize());
        assertEquals("默认批处理大小应该正确", 500, batchConfig.getDefaultBatchSize());
        assertEquals("最优批处理大小应该正确", 1000, batchConfig.getOptimalBatchSize());
        assertEquals("最大并发批次数应该正确", 3, batchConfig.getMaxConcurrentBatches());
        assertTrue("并行处理应该启用", batchConfig.isEnableParallelProcessing());
        assertEquals("内存阈值应该正确", 512, batchConfig.getMemoryThresholdMb());
        assertTrue("内存监控应该启用", batchConfig.isEnableMemoryMonitoring());
        assertEquals("批次超时时间应该正确", 600, batchConfig.getBatchTimeoutSeconds());
        assertEquals("总超时时间应该正确", 3600, batchConfig.getTotalTimeoutSeconds());
        assertTrue("部分成功应该启用", batchConfig.isEnablePartialSuccess());
        assertTrue("进度跟踪应该启用", batchConfig.isEnableProgressTracking());
        assertEquals("进度报告间隔应该正确", 100, batchConfig.getProgressReportInterval());
    }

    @Test
    public void testApiSelectorIntegration() {
        // 测试API选择器集成
        assertNotNull("ApiSelector应该被正确注入", apiSelector);
        
        // 测试配置验证
        assertTrue("API选择器配置应该有效", apiSelector.validateConfiguration());
        
        // 测试单文件选择
        List<String> singleFile = createFileList(1);
        ApiSelector.ApiSelection selection1 = apiSelector.selectOptimalApi(singleFile);
        assertEquals("单文件应该选择SINGLE API", ApiSelector.ApiType.SINGLE, selection1.getApiType());
        
        // 测试小批量选择
        List<String> smallBatch = createFileList(100);
        ApiSelector.ApiSelection selection2 = apiSelector.selectOptimalApi(smallBatch);
        assertEquals("小批量应该选择BATCH API", ApiSelector.ApiType.BATCH, selection2.getApiType());
        
        // 测试大批量选择
        List<String> largeBatch = createFileList(800);
        ApiSelector.ApiSelection selection3 = apiSelector.selectOptimalApi(largeBatch);
        assertEquals("大批量应该选择LARGE_BATCH API", ApiSelector.ApiType.LARGE_BATCH, selection3.getApiType());
        
        // 测试超大批量选择
        List<String> megaBatch = createFileList(3000);
        ApiSelector.ApiSelection selection4 = apiSelector.selectOptimalApi(megaBatch);
        assertEquals("超大批量应该选择MEGA_BATCH_ASYNC API", ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection4.getApiType());
    }

    @Test
    public void testCacheRefreshServiceIntegration() {
        // 测试缓存刷新服务集成
        assertNotNull("CacheRefreshService应该被正确注入", cacheRefreshService);
        
        // 测试统计信息
        String stats = cacheRefreshService.getStatistics();
        assertNotNull("统计信息不应该为空", stats);
        assertTrue("统计信息应该包含成功计数", stats.contains("成功"));
        assertTrue("统计信息应该包含失败计数", stats.contains("失败"));
        
        // 测试文件收集功能
        cacheRefreshService.collectSuccessfulFile("test1.jpg");
        cacheRefreshService.collectSuccessfulFile("test2.jpg");
        cacheRefreshService.collectSuccessfulFile("test3.jpg");
        
        // 验证文件收集
        String updatedStats = cacheRefreshService.getStatistics();
        assertTrue("统计信息应该反映收集的文件", updatedStats.contains("待刷新文件") && !updatedStats.contains("待刷新文件: 0"));
    }

    @Test
    public void testEndToEndIntegration() {
        // 测试端到端集成
        
        // 1. 收集文件
        List<String> testFiles = createFileList(10);
        for (String file : testFiles) {
            cacheRefreshService.collectSuccessfulFile(file);
        }
        
        // 2. 验证API选择
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(testFiles);
        assertEquals("10个文件应该选择BATCH API", ApiSelector.ApiType.BATCH, selection.getApiType());
        assertFalse("BATCH API不应该是异步的", selection.isAsync());
        
        // 3. 验证统计信息
        String stats = cacheRefreshService.getStatistics();
        assertTrue("统计信息应该显示待刷新文件", stats.contains("待刷新文件") && !stats.contains("待刷新文件: 0"));
        
        // 4. 测试批量大小计算
        int optimalBatchSize = apiSelector.calculateOptimalBatchSize(testFiles.size());
        assertEquals("10个文件的最优批次大小应该是10", 10, optimalBatchSize);
        
        // 5. 测试分批需求检查
        assertFalse("10个文件不需要分批处理", apiSelector.needsBatching(testFiles.size()));
        
        // 6. 测试选择统计
        String selectionStats = apiSelector.getSelectionStats(testFiles.size());
        assertTrue("选择统计应该包含文件数量", selectionStats.contains("文件数量=10"));
        assertTrue("选择统计应该包含API类型", selectionStats.contains("选择API=batch"));
    }

    @Test
    public void testLargeBatchScenario() {
        // 测试大批量场景
        List<String> largeBatch = createFileList(1500);
        
        // 验证API选择
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(largeBatch);
        assertEquals("1500个文件应该选择LARGE_BATCH API", ApiSelector.ApiType.LARGE_BATCH, selection.getApiType());
        assertFalse("LARGE_BATCH API不应该是异步的", selection.isAsync());
        
        // 验证批量大小计算
        int optimalBatchSize = apiSelector.calculateOptimalBatchSize(largeBatch.size());
        assertEquals("1500个文件的最优批次大小应该是1000", 1000, optimalBatchSize);
        
        // 验证分批需求
        assertFalse("1500个文件不需要分批处理", apiSelector.needsBatching(largeBatch.size()));
    }

    @Test
    public void testMegaBatchScenario() {
        // 测试超大批量场景
        List<String> megaBatch = createFileList(3500);
        
        // 验证API选择
        ApiSelector.ApiSelection selection = apiSelector.selectOptimalApi(megaBatch);
        assertEquals("3500个文件应该选择MEGA_BATCH_ASYNC API", ApiSelector.ApiType.MEGA_BATCH_ASYNC, selection.getApiType());
        assertTrue("MEGA_BATCH_ASYNC API应该是异步的", selection.isAsync());
        
        // 验证批量大小计算
        int optimalBatchSize = apiSelector.calculateOptimalBatchSize(megaBatch.size());
        assertEquals("3500个文件的最优批次大小应该是1166", 1166, optimalBatchSize);
        
        // 验证分批需求
        assertFalse("3500个文件不需要分批处理", apiSelector.needsBatching(megaBatch.size()));
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
