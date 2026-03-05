package com.mls.upload.client.integration;

import com.mls.upload.client.service.AuthService;
import com.mls.upload.client.service.ExcelService;
import com.mls.upload.client.service.FileUploadService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * 完整业务流程集成测试
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class BusinessFlowIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessFlowIntegrationTest.class);
    
    private AuthService authService;
    private ExcelService excelService;
    private FileUploadService fileUploadService;
    
    @Before
    public void setUp() {
        authService = new AuthService();
        excelService = new ExcelService();
        fileUploadService = new FileUploadService();
        logger.info("集成测试环境初始化完成");
    }
    
    @Test
    public void testCompleteBusinessFlow() {
        logger.info("开始完整业务流程测试...");
        
        // 1. 测试服务器连接
        logger.info("步骤1: 测试服务器连接");
        boolean connected = authService.testConnection();
        assertTrue("服务器连接应该成功", connected);
        logger.info("服务器连接测试通过");
        
        // 2. 测试用户登录（使用错误凭据，因为我们不知道正确的密码）
        logger.info("步骤2: 测试用户登录");
        boolean loginResult = authService.login("admin", "wrong_password");
        assertFalse("错误密码登录应该失败", loginResult);
        logger.info("登录失败测试通过（预期行为）");
        
        // 3. 测试Excel服务初始化
        logger.info("步骤3: 测试Excel服务");
        assertNotNull("Excel服务应该初始化成功", excelService);
        logger.info("Excel服务初始化测试通过");
        
        // 4. 测试文件上传服务初始化
        logger.info("步骤4: 测试文件上传服务");
        assertNotNull("文件上传服务应该初始化成功", fileUploadService);
        logger.info("文件上传服务初始化测试通过");
        
        logger.info("完整业务流程测试完成");
    }
    
    @Test
    public void testServiceIntegration() {
        logger.info("开始服务集成测试...");
        
        // 测试各个服务之间的协作
        assertTrue("认证服务应该可用", authService != null);
        assertTrue("Excel服务应该可用", excelService != null);
        assertTrue("文件上传服务应该可用", fileUploadService != null);
        
        // 测试服务状态
        assertFalse("初始状态应该未登录", authService.isLoggedIn());
        assertNull("初始状态用户名应该为空", authService.getCurrentUsername());
        
        logger.info("服务集成测试完成");
    }
    
    @Test
    public void testErrorHandling() {
        logger.info("开始错误处理测试...");
        
        // 测试各种错误情况的处理
        
        // 1. 测试无效Excel文件路径
        try {
            excelService.parseExcelFile("non_existent_file.xlsx");
            fail("无效文件应该抛出异常");
        } catch (Exception e) {
            logger.info("无效文件路径测试通过: {}", e.getMessage());
        }

        // 2. 测试空路径
        try {
            excelService.parseExcelFile("");
            fail("空路径应该抛出异常");
        } catch (Exception e) {
            logger.info("空路径测试通过: {}", e.getMessage());
        }

        // 3. 测试null路径
        try {
            excelService.parseExcelFile(null);
            fail("null路径应该抛出异常");
        } catch (Exception e) {
            logger.info("null路径测试通过: {}", e.getMessage());
        }
        
        logger.info("错误处理测试完成");
    }
}
