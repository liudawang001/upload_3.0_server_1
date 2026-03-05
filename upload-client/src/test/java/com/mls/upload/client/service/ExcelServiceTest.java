package com.mls.upload.client.service;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * Excel服务测试类
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ExcelServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelServiceTest.class);
    
    private ExcelService excelService;
    
    @Before
    public void setUp() {
        excelService = new ExcelService();
    }
    
    @Test
    public void testServiceInitialization() {
        assertNotNull("Excel服务应该成功初始化", excelService);
        logger.info("Excel服务初始化测试通过");
    }
    
    @Test
    public void testValidateExcelFileWithNullPath() {
        boolean result = excelService.validateExcelFile(null);
        assertFalse("空路径应该验证失败", result);
        logger.info("空路径验证测试通过");
    }
    
    @Test
    public void testValidateExcelFileWithEmptyPath() {
        boolean result = excelService.validateExcelFile("");
        assertFalse("空字符串路径应该验证失败", result);
        logger.info("空字符串路径验证测试通过");
    }
    
    @Test
    public void testValidateExcelFileWithInvalidExtension() {
        boolean result = excelService.validateExcelFile("test.txt");
        assertFalse("非Excel文件应该验证失败", result);
        logger.info("无效扩展名验证测试通过");
    }
    
    @Test
    public void testValidateExcelFileWithNonExistentFile() {
        boolean result = excelService.validateExcelFile("non_existent_file.xlsx");
        assertFalse("不存在的文件应该验证失败", result);
        logger.info("不存在文件验证测试通过");
    }
    
    @Test
    public void testParseExcelFileWithNullPath() {
        try {
            excelService.parseExcelFile(null);
            fail("空路径应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该抛出IllegalArgumentException", e instanceof IllegalArgumentException);
            logger.info("空路径解析测试通过: {}", e.getMessage());
        }
    }
    
    @Test
    public void testParseExcelFileWithEmptyPath() {
        try {
            excelService.parseExcelFile("");
            fail("空字符串路径应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该抛出IllegalArgumentException", e instanceof IllegalArgumentException);
            logger.info("空字符串路径解析测试通过: {}", e.getMessage());
        }
    }
    
    @Test
    public void testParseExcelFileWithInvalidExtension() {
        try {
            excelService.parseExcelFile("test.txt");
            fail("无效扩展名应该抛出异常");
        } catch (Exception e) {
            assertTrue("应该抛出IllegalArgumentException", e instanceof IllegalArgumentException);
            logger.info("无效扩展名解析测试通过: {}", e.getMessage());
        }
    }
}
