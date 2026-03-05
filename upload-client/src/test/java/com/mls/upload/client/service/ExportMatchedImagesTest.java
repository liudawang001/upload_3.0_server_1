package com.mls.upload.client.service;

import com.mls.upload.client.model.entity.ExcelRowData;
import com.mls.upload.client.model.vo.LogEntry;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 导出匹配图片功能测试
 */
public class ExportMatchedImagesTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportMatchedImagesTest.class);
    
    private List<LogEntry> testEntries;
    private File tempImageFolder;
    private File testImage1;
    private File testImage2;
    
    @Before
    public void setUp() throws IOException {
        // 创建临时测试文件夹
        tempImageFolder = Files.createTempDirectory("test_images").toFile();
        
        // 创建测试图片文件
        testImage1 = new File(tempImageFolder, "test_image_1.jpg");
        testImage2 = new File(tempImageFolder, "test_image_2.png");
        
        // 创建空的测试文件
        testImage1.createNewFile();
        testImage2.createNewFile();
        
        // 写入一些测试内容
        Files.write(testImage1.toPath(), "test image 1 content".getBytes());
        Files.write(testImage2.toPath(), "test image 2 content".getBytes());
        
        // 创建测试数据
        testEntries = createTestData();
    }
    
    @Test
    public void testMatchedEntriesFiltering() {
        logger.info("开始测试匹配记录筛选功能");

        // 筛选匹配的记录（不需要上传成功）
        List<LogEntry> matchedEntries = testEntries.stream()
            .filter(this::isMatchedEntry)
            .filter(entry -> entry.getFilePath() != null && !entry.getFilePath().isEmpty())
            .collect(java.util.stream.Collectors.toList());

        // 验证筛选结果
        assertEquals("应该有3个匹配的记录", 3, matchedEntries.size());

        // 验证筛选的记录确实是匹配的
        for (LogEntry entry : matchedEntries) {
            assertEquals("匹配状态应该是匹配", "匹配", entry.getMatchStatus());
            assertNotNull("文件路径不应该为空", entry.getFilePath());
            assertFalse("文件路径不应该为空字符串", entry.getFilePath().isEmpty());
        }

        // 验证包含了不同上传状态的匹配记录
        long uploadedCount = matchedEntries.stream()
            .filter(entry -> "成功".equals(entry.getUploadStatus()))
            .count();
        long notUploadedCount = matchedEntries.stream()
            .filter(entry -> !"成功".equals(entry.getUploadStatus()))
            .count();

        assertEquals("应该有1个已上传的匹配记录", 1, uploadedCount);
        assertEquals("应该有2个未上传的匹配记录", 2, notUploadedCount);

        logger.info("匹配记录筛选测试通过");
    }
    
    @Test
    public void testImageFileExistence() {
        logger.info("开始测试图片文件存在性检查");
        
        // 验证测试图片文件存在
        assertTrue("测试图片1应该存在", testImage1.exists());
        assertTrue("测试图片2应该存在", testImage2.exists());
        assertTrue("测试图片1应该是文件", testImage1.isFile());
        assertTrue("测试图片2应该是文件", testImage2.isFile());
        
        // 验证文件内容
        try {
            byte[] content1 = Files.readAllBytes(testImage1.toPath());
            byte[] content2 = Files.readAllBytes(testImage2.toPath());
            
            assertTrue("测试图片1应该有内容", content1.length > 0);
            assertTrue("测试图片2应该有内容", content2.length > 0);
            
            assertEquals("测试图片1内容应该正确", "test image 1 content", new String(content1));
            assertEquals("测试图片2内容应该正确", "test image 2 content", new String(content2));
            
        } catch (IOException e) {
            fail("读取测试图片内容失败: " + e.getMessage());
        }
        
        logger.info("图片文件存在性检查测试通过");
    }
    
    @Test
    public void testTargetFolderCreation() {
        logger.info("开始测试目标文件夹创建功能");
        
        // 创建目标文件夹
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String targetFolderName = "匹配图片_" + timestamp;
        File targetFolder = new File(tempImageFolder, targetFolderName);
        
        // 验证文件夹不存在
        assertFalse("目标文件夹初始时不应该存在", targetFolder.exists());
        
        // 创建文件夹
        boolean created = targetFolder.mkdirs();
        assertTrue("应该能够创建目标文件夹", created);
        assertTrue("目标文件夹应该存在", targetFolder.exists());
        assertTrue("目标文件夹应该是目录", targetFolder.isDirectory());
        
        // 验证文件夹名称格式
        assertTrue("文件夹名称应该包含前缀", targetFolder.getName().startsWith("匹配图片_"));
        assertTrue("文件夹名称应该包含时间戳", targetFolder.getName().length() > "匹配图片_".length());
        
        logger.info("目标文件夹创建功能测试通过");
    }
    
    /**
     * 判断是否为匹配的条目（不考虑上传状态）
     */
    private boolean isMatchedEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        return "匹配".equals(entry.getMatchStatus());
    }

    /**
     * 判断是否为匹配成功的条目
     */
    private boolean isMatchedSuccessEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        boolean uploadSuccess = "成功".equals(entry.getUploadStatus());
        boolean matchSuccess = "匹配".equals(entry.getMatchStatus());

        return uploadSuccess && matchSuccess;
    }
    
    /**
     * 创建测试数据
     */
    private List<LogEntry> createTestData() {
        // 匹配成功的记录1
        LogEntry entry1 = new LogEntry();
        entry1.setIndex(1);
        entry1.setProductCode("PROD001");
        entry1.setFilename("test_image_1.jpg");
        entry1.setFilePath(testImage1.getAbsolutePath());
        entry1.setUploadStatus("成功");
        entry1.setMatchStatus("匹配");
        entry1.setProgress("100%");
        entry1.setMessage("上传成功");
        entry1.setTime("12:00:00");
        
        // 匹配但未上传的记录
        LogEntry entry2 = new LogEntry();
        entry2.setIndex(2);
        entry2.setProductCode("PROD002");
        entry2.setFilename("test_image_2.png");
        entry2.setFilePath(testImage2.getAbsolutePath());
        entry2.setUploadStatus("待上传");
        entry2.setMatchStatus("匹配");
        entry2.setProgress("0%");
        entry2.setMessage("匹配成功，等待上传");
        entry2.setTime("12:01:00");
        
        // 上传失败的记录
        LogEntry entry3 = new LogEntry();
        entry3.setIndex(3);
        entry3.setProductCode("PROD003");
        entry3.setFilename("failed_image.jpg");
        entry3.setFilePath("/path/to/failed_image.jpg");
        entry3.setUploadStatus("失败");
        entry3.setMatchStatus("匹配");
        entry3.setProgress("0%");
        entry3.setMessage("上传失败");
        entry3.setTime("12:02:00");
        
        // 匹配失败的记录
        LogEntry entry4 = new LogEntry();
        entry4.setIndex(4);
        entry4.setProductCode("PROD004");
        entry4.setFilename("mismatch_image.jpg");
        entry4.setFilePath("/path/to/mismatch_image.jpg");
        entry4.setUploadStatus("成功");
        entry4.setMatchStatus("不匹配");
        entry4.setProgress("100%");
        entry4.setMessage("比对不匹配");
        entry4.setTime("12:03:00");
        
        // 无文件路径的记录
        LogEntry entry5 = new LogEntry();
        entry5.setIndex(5);
        entry5.setProductCode("PROD005");
        entry5.setFilename("no_path_image.jpg");
        entry5.setFilePath(null);
        entry5.setUploadStatus("成功");
        entry5.setMatchStatus("匹配");
        entry5.setProgress("100%");
        entry5.setMessage("无文件路径");
        entry5.setTime("12:04:00");
        
        return Arrays.asList(entry1, entry2, entry3, entry4, entry5);
    }
}
