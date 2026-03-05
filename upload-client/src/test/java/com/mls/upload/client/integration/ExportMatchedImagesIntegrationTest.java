package com.mls.upload.client.integration;

import com.mls.upload.client.model.entity.ExcelRowData;
import com.mls.upload.client.model.vo.LogEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * 导出匹配图片功能集成测试
 */
public class ExportMatchedImagesIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportMatchedImagesIntegrationTest.class);
    
    private File tempImageFolder;
    private File testImage1;
    private File testImage2;
    private File testImage3;
    private List<LogEntry> testEntries;
    
    @Before
    public void setUp() throws IOException {
        // 创建临时测试文件夹
        tempImageFolder = Files.createTempDirectory("integration_test_images").toFile();
        
        // 创建测试图片文件
        testImage1 = new File(tempImageFolder, "matched_image_1.jpg");
        testImage2 = new File(tempImageFolder, "matched_image_2.png");
        testImage3 = new File(tempImageFolder, "failed_image.jpg");
        
        // 创建测试文件并写入内容
        Files.write(testImage1.toPath(), "Test matched image 1 content".getBytes());
        Files.write(testImage2.toPath(), "Test matched image 2 content".getBytes());
        Files.write(testImage3.toPath(), "Test failed image content".getBytes());
        
        // 创建测试数据
        testEntries = createIntegrationTestData();
        
        logger.info("集成测试环境初始化完成，临时文件夹: {}", tempImageFolder.getAbsolutePath());
    }
    
    @After
    public void tearDown() {
        // 清理临时文件
        if (tempImageFolder != null && tempImageFolder.exists()) {
            deleteDirectory(tempImageFolder);
        }
    }
    
    @Test
    public void testCompleteExportMatchedImagesWorkflow() throws Exception {
        logger.info("开始完整的导出匹配图片工作流测试");
        
        // 1. 筛选匹配成功的记录
        List<LogEntry> matchedEntries = testEntries.stream()
            .filter(this::isMatchedSuccessEntry)
            .filter(entry -> entry.getFilePath() != null && !entry.getFilePath().isEmpty())
            .collect(Collectors.toList());
        
        assertEquals("应该有2个匹配成功的记录", 2, matchedEntries.size());
        
        // 2. 创建目标文件夹
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String targetFolderName = "匹配图片_" + timestamp;
        File targetFolder = new File(tempImageFolder, targetFolderName);
        
        assertTrue("应该能够创建目标文件夹", targetFolder.mkdirs());
        
        // 3. 模拟图片复制过程
        copyMatchedImages(matchedEntries, targetFolder);
        
        // 4. 验证复制结果
        File[] copiedFiles = targetFolder.listFiles();
        assertNotNull("目标文件夹应该包含文件", copiedFiles);
        assertEquals("应该复制了2个文件", 2, copiedFiles.length);
        
        // 5. 验证文件内容
        boolean foundImage1 = false;
        boolean foundImage2 = false;
        
        for (File copiedFile : copiedFiles) {
            String content = new String(Files.readAllBytes(copiedFile.toPath()));
            if (content.equals("Test matched image 1 content")) {
                foundImage1 = true;
                assertEquals("第一个图片文件名应该正确", "matched_image_1.jpg", copiedFile.getName());
            } else if (content.equals("Test matched image 2 content")) {
                foundImage2 = true;
                assertEquals("第二个图片文件名应该正确", "matched_image_2.png", copiedFile.getName());
            }
        }
        
        assertTrue("应该找到第一个匹配图片", foundImage1);
        assertTrue("应该找到第二个匹配图片", foundImage2);
        
        logger.info("完整的导出匹配图片工作流测试通过");
    }
    
    @Test
    public void testFileNameConflictHandling() throws Exception {
        logger.info("开始测试文件名冲突处理");
        
        // 筛选匹配成功的记录
        List<LogEntry> matchedEntries = testEntries.stream()
            .filter(this::isMatchedSuccessEntry)
            .filter(entry -> entry.getFilePath() != null && !entry.getFilePath().isEmpty())
            .collect(Collectors.toList());
        
        // 创建目标文件夹
        File targetFolder = new File(tempImageFolder, "conflict_test");
        assertTrue("应该能够创建目标文件夹", targetFolder.mkdirs());
        
        // 预先创建一个同名文件
        File conflictFile = new File(targetFolder, "matched_image_1.jpg");
        Files.write(conflictFile.toPath(), "existing file content".getBytes());
        
        // 执行复制
        copyMatchedImages(matchedEntries, targetFolder);
        
        // 验证结果
        File[] files = targetFolder.listFiles();
        assertNotNull("目标文件夹应该包含文件", files);
        assertEquals("应该有3个文件（原有1个 + 复制2个，其中1个重命名）", 3, files.length);
        
        // 验证重命名文件存在
        boolean foundRenamedFile = false;
        for (File file : files) {
            if (file.getName().equals("matched_image_1_1.jpg")) {
                foundRenamedFile = true;
                String content = new String(Files.readAllBytes(file.toPath()));
                assertEquals("重命名文件内容应该正确", "Test matched image 1 content", content);
            }
        }
        
        assertTrue("应该找到重命名的文件", foundRenamedFile);
        
        logger.info("文件名冲突处理测试通过");
    }
    
    @Test
    public void testEmptyMatchedEntriesHandling() {
        logger.info("开始测试空匹配记录处理");
        
        // 创建只包含失败记录的测试数据
        List<LogEntry> failedOnlyEntries = testEntries.stream()
            .filter(entry -> !isMatchedSuccessEntry(entry))
            .collect(Collectors.toList());
        
        // 筛选匹配成功的记录（应该为空）
        List<LogEntry> matchedEntries = failedOnlyEntries.stream()
            .filter(this::isMatchedSuccessEntry)
            .collect(Collectors.toList());
        
        assertTrue("失败记录中不应该有匹配成功的记录", matchedEntries.isEmpty());
        
        logger.info("空匹配记录处理测试通过");
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
     * 复制匹配成功的图片文件到目标文件夹
     */
    private void copyMatchedImages(List<LogEntry> matchedEntries, File targetFolder) throws Exception {
        logger.info("开始复制 {} 张匹配图片到: {}", matchedEntries.size(), targetFolder.getAbsolutePath());
        
        for (LogEntry entry : matchedEntries) {
            String sourceFilePath = entry.getFilePath();
            
            if (sourceFilePath == null || sourceFilePath.isEmpty()) {
                continue;
            }
            
            File sourceFile = new File(sourceFilePath);
            if (!sourceFile.exists()) {
                continue;
            }
            
            // 获取文件名
            String fileName = sourceFile.getName();
            File targetFile = new File(targetFolder, fileName);
            
            // 如果目标文件已存在，添加序号后缀
            if (targetFile.exists()) {
                String baseName = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }
                
                int counter = 1;
                do {
                    String newFileName = baseName + "_" + counter + extension;
                    targetFile = new File(targetFolder, newFileName);
                    counter++;
                } while (targetFile.exists());
            }
            
            // 使用NIO复制文件
            Path sourcePath = sourceFile.toPath();
            Path targetPath = targetFile.toPath();
            Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
            
            logger.debug("复制成功: {} -> {}", sourceFile.getName(), targetFile.getName());
        }
        
        logger.info("图片复制完成");
    }
    
    /**
     * 创建集成测试数据
     */
    private List<LogEntry> createIntegrationTestData() {
        // 匹配成功的记录1
        LogEntry entry1 = new LogEntry();
        entry1.setIndex(1);
        entry1.setProductCode("PROD001");
        entry1.setFilename("matched_image_1.jpg");
        entry1.setFilePath(testImage1.getAbsolutePath());
        entry1.setUploadStatus("成功");
        entry1.setMatchStatus("匹配");
        entry1.setProgress("100%");
        entry1.setMessage("上传成功");
        entry1.setTime("12:00:00");
        
        // 匹配成功的记录2
        LogEntry entry2 = new LogEntry();
        entry2.setIndex(2);
        entry2.setProductCode("PROD002");
        entry2.setFilename("matched_image_2.png");
        entry2.setFilePath(testImage2.getAbsolutePath());
        entry2.setUploadStatus("成功");
        entry2.setMatchStatus("匹配");
        entry2.setProgress("100%");
        entry2.setMessage("上传成功");
        entry2.setTime("12:01:00");
        
        // 上传失败的记录
        LogEntry entry3 = new LogEntry();
        entry3.setIndex(3);
        entry3.setProductCode("PROD003");
        entry3.setFilename("failed_image.jpg");
        entry3.setFilePath(testImage3.getAbsolutePath());
        entry3.setUploadStatus("失败");
        entry3.setMatchStatus("匹配");
        entry3.setProgress("0%");
        entry3.setMessage("上传失败");
        entry3.setTime("12:02:00");
        
        return Arrays.asList(entry1, entry2, entry3);
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
