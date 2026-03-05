package com.mls.upload.client.service;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * 认证服务测试类
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class AuthServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);
    
    private AuthService authService;
    
    @Before
    public void setUp() {
        authService = new AuthService();
    }
    
    @Test
    public void testServiceInitialization() {
        assertNotNull("认证服务应该成功初始化", authService);
        assertFalse("初始状态应该未登录", authService.isLoggedIn());
        assertNull("初始状态用户名应该为空", authService.getCurrentUsername());
        logger.info("认证服务初始化测试通过");
    }
    
    @Test
    public void testConnectionTest() {
        // 测试服务器连接（可能失败，因为服务器可能未启动）
        boolean connected = authService.testConnection();
        logger.info("服务器连接测试结果: {}", connected ? "成功" : "失败");
        
        // 这里不做断言，因为服务器可能未启动
        // 只是验证方法能正常执行不抛异常
    }
    
    @Test
    public void testLoginWithInvalidCredentials() {
        // 测试无效凭据登录（应该返回false，不抛异常）
        boolean result = authService.login("invalid_user", "invalid_password");
        assertFalse("无效凭据登录应该返回false", result);
        assertFalse("登录失败后状态应该仍为未登录", authService.isLoggedIn());
        logger.info("无效凭据登录测试通过");
    }
    
    @Test
    public void testLogout() {
        // 测试登出功能
        authService.logout();
        assertFalse("登出后状态应该为未登录", authService.isLoggedIn());
        assertNull("登出后用户名应该为空", authService.getCurrentUsername());
        assertNull("登出后token应该为空", authService.getCurrentToken());
        logger.info("登出功能测试通过");
    }
}
