package com.mls.upload.client.controller;

import com.mls.upload.client.model.vo.LogEntry;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 导出按钮启用条件测试
 */
public class ExportButtonEnableTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportButtonEnableTest.class);
    
    private List<LogEntry> testEntries;
    
    @Before
    public void setUp() {
        // 创建测试数据
        testEntries = createTestData();
    }
    
    @Test
    public void testIsMatchedEntryLogic() {
        logger.info("开始测试匹配记录判断逻辑");
        
        // 测试匹配但未上传的记录
        LogEntry matchedNotUploaded = testEntries.get(0);
        assertTrue("匹配但未上传的记录应该被isMatchedEntry识别", isMatchedEntry(matchedNotUploaded));
        assertFalse("匹配但未上传的记录不应该被isMatchedSuccessEntry识别", isMatchedSuccessEntry(matchedNotUploaded));
        
        // 测试匹配且上传成功的记录
        LogEntry matchedAndUploaded = testEntries.get(1);
        assertTrue("匹配且上传成功的记录应该被isMatchedEntry识别", isMatchedEntry(matchedAndUploaded));
        assertTrue("匹配且上传成功的记录应该被isMatchedSuccessEntry识别", isMatchedSuccessEntry(matchedAndUploaded));
        
        // 测试不匹配的记录
        LogEntry notMatched = testEntries.get(2);
        assertFalse("不匹配的记录不应该被isMatchedEntry识别", isMatchedEntry(notMatched));
        assertFalse("不匹配的记录不应该被isMatchedSuccessEntry识别", isMatchedSuccessEntry(notMatched));
        
        logger.info("匹配记录判断逻辑测试通过");
    }
    
    @Test
    public void testButtonEnableConditions() {
        logger.info("开始测试按钮启用条件");
        
        // 模拟按钮启用条件检查
        boolean hasMatchedRecords = testEntries.stream()
            .anyMatch(this::isMatchedEntry);
        
        boolean hasMatchedSuccessRecords = testEntries.stream()
            .anyMatch(this::isMatchedSuccessEntry);
        
        // 验证结果
        assertTrue("应该有匹配的记录（用于按钮启用）", hasMatchedRecords);
        assertTrue("应该有匹配成功的记录（用于实际导出）", hasMatchedSuccessRecords);
        
        // 统计各种类型的记录
        long matchedCount = testEntries.stream()
            .filter(this::isMatchedEntry)
            .count();
        
        long matchedSuccessCount = testEntries.stream()
            .filter(this::isMatchedSuccessEntry)
            .count();
        
        long matchedButNotUploadedCount = testEntries.stream()
            .filter(this::isMatchedEntry)
            .filter(entry -> !"成功".equals(entry.getUploadStatus()))
            .count();
        
        assertEquals("应该有3个匹配的记录", 3, matchedCount);
        assertEquals("应该有1个匹配成功的记录", 1, matchedSuccessCount);
        assertEquals("应该有2个匹配但未上传成功的记录", 2, matchedButNotUploadedCount);
        
        logger.info("按钮启用条件测试通过 - 匹配记录: {}, 匹配成功记录: {}, 匹配但未上传: {}", 
                   matchedCount, matchedSuccessCount, matchedButNotUploadedCount);
    }
    
    @Test
    public void testExportFilterLogic() {
        logger.info("开始测试导出筛选逻辑");
        
        // 模拟导出时的筛选逻辑
        List<LogEntry> exportableEntries = testEntries.stream()
            .filter(this::isMatchedSuccessEntry)
            .filter(entry -> entry.getFilePath() != null && !entry.getFilePath().isEmpty())
            .collect(java.util.stream.Collectors.toList());
        
        assertEquals("应该只有1个可导出的记录", 1, exportableEntries.size());
        
        LogEntry exportableEntry = exportableEntries.get(0);
        assertEquals("可导出记录的上传状态应该是成功", "成功", exportableEntry.getUploadStatus());
        assertEquals("可导出记录的匹配状态应该是匹配", "匹配", exportableEntry.getMatchStatus());
        assertNotNull("可导出记录应该有文件路径", exportableEntry.getFilePath());
        
        logger.info("导出筛选逻辑测试通过");
    }
    
    @Test
    public void testEmptyMatchedRecordsScenario() {
        logger.info("开始测试无匹配记录场景");
        
        // 创建只包含不匹配记录的测试数据
        LogEntry notMatched1 = new LogEntry();
        notMatched1.setMatchStatus("不匹配");
        notMatched1.setUploadStatus("成功");
        
        LogEntry notMatched2 = new LogEntry();
        notMatched2.setMatchStatus("缺少图片");
        notMatched2.setUploadStatus("失败");
        
        List<LogEntry> noMatchEntries = Arrays.asList(notMatched1, notMatched2);
        
        boolean hasMatchedRecords = noMatchEntries.stream()
            .anyMatch(this::isMatchedEntry);
        
        assertFalse("不应该有匹配的记录", hasMatchedRecords);
        
        logger.info("无匹配记录场景测试通过");
    }
    
    /**
     * 判断是否为匹配的条目（不考虑上传状态）
     */
    private boolean isMatchedEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }
        
        // 只检查比对状态是否匹配，不考虑上传状态
        return "匹配".equals(entry.getMatchStatus());
    }
    
    /**
     * 判断是否为匹配成功的条目（上传成功且匹配成功）
     */
    private boolean isMatchedSuccessEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }
        
        // 检查上传状态是否成功且比对状态是否匹配
        boolean uploadSuccess = "成功".equals(entry.getUploadStatus());
        boolean matchSuccess = "匹配".equals(entry.getMatchStatus());
        
        return uploadSuccess && matchSuccess;
    }
    
    /**
     * 创建测试数据
     */
    private List<LogEntry> createTestData() {
        // 匹配但未上传成功的记录
        LogEntry entry1 = new LogEntry();
        entry1.setIndex(1);
        entry1.setProductCode("PROD001");
        entry1.setFilename("matched_not_uploaded.jpg");
        entry1.setFilePath("/path/to/matched_not_uploaded.jpg");
        entry1.setUploadStatus("待上传");
        entry1.setMatchStatus("匹配");
        entry1.setProgress("0%");
        entry1.setMessage("匹配成功，等待上传");
        entry1.setTime("12:00:00");
        
        // 匹配且上传成功的记录
        LogEntry entry2 = new LogEntry();
        entry2.setIndex(2);
        entry2.setProductCode("PROD002");
        entry2.setFilename("matched_uploaded.jpg");
        entry2.setFilePath("/path/to/matched_uploaded.jpg");
        entry2.setUploadStatus("成功");
        entry2.setMatchStatus("匹配");
        entry2.setProgress("100%");
        entry2.setMessage("上传成功");
        entry2.setTime("12:01:00");
        
        // 不匹配的记录
        LogEntry entry3 = new LogEntry();
        entry3.setIndex(3);
        entry3.setProductCode("PROD003");
        entry3.setFilename("not_matched.jpg");
        entry3.setFilePath("/path/to/not_matched.jpg");
        entry3.setUploadStatus("成功");
        entry3.setMatchStatus("不匹配");
        entry3.setProgress("100%");
        entry3.setMessage("比对不匹配");
        entry3.setTime("12:02:00");
        
        // 上传失败的记录
        LogEntry entry4 = new LogEntry();
        entry4.setIndex(4);
        entry4.setProductCode("PROD004");
        entry4.setFilename("upload_failed.jpg");
        entry4.setFilePath("/path/to/upload_failed.jpg");
        entry4.setUploadStatus("失败");
        entry4.setMatchStatus("匹配");
        entry4.setProgress("0%");
        entry4.setMessage("上传失败");
        entry4.setTime("12:03:00");
        
        return Arrays.asList(entry1, entry2, entry3, entry4);
    }
}
