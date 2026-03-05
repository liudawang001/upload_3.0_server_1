package com.mls.upload.client.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 用户凭据管理器测试类
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class CredentialManagerTest {

    private CredentialManager credentialManager;

    @Before
    public void setUp() {
        credentialManager = new CredentialManager();
        // 清除之前的测试数据
        credentialManager.clearCredentials();
    }

    @After
    public void tearDown() {
        // 清理测试数据
        if (credentialManager != null) {
            credentialManager.clearCredentials();
        }
    }

    @Test
    public void testSaveAndLoadCredentials() {
        // 测试数据
        String username = "testuser";
        String password = "testpassword123";
        boolean rememberPassword = true;

        // 保存凭据
        credentialManager.saveCredentials(username, password, rememberPassword);

        // 验证保存状态
        assertTrue("应该有保存的凭据", credentialManager.hasCredentials());
        assertTrue("应该记住密码", credentialManager.isRememberPassword());

        // 读取凭据
        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();

        // 验证读取结果
        assertNotNull("应该能读取到保存的凭据", savedCredentials);
        assertEquals("用户名应该匹配", username, savedCredentials.getUsername());
        assertEquals("密码应该匹配", password, savedCredentials.getPassword());
        assertTrue("记住密码选项应该为true", savedCredentials.isRememberPassword());
    }

    @Test
    public void testSaveCredentialsWithRememberPasswordFalse() {
        // 测试数据
        String username = "testuser";
        String password = "testpassword123";
        boolean rememberPassword = false;

        // 保存凭据（不记住密码）
        credentialManager.saveCredentials(username, password, rememberPassword);

        // 验证保存状态
        assertFalse("不应该有保存的凭据", credentialManager.hasCredentials());
        assertFalse("不应该记住密码", credentialManager.isRememberPassword());

        // 读取凭据
        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();

        // 验证读取结果
        assertNull("不应该能读取到凭据", savedCredentials);
    }

    @Test
    public void testClearCredentials() {
        // 先保存一些凭据
        String username = "testuser";
        String password = "testpassword123";
        boolean rememberPassword = true;

        credentialManager.saveCredentials(username, password, rememberPassword);
        assertTrue("应该有保存的凭据", credentialManager.hasCredentials());

        // 清除凭据
        credentialManager.clearCredentials();

        // 验证清除结果
        assertFalse("不应该有保存的凭据", credentialManager.hasCredentials());
        assertFalse("不应该记住密码", credentialManager.isRememberPassword());

        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();
        assertNull("不应该能读取到凭据", savedCredentials);
    }

    @Test
    public void testPasswordEncryption() {
        // 测试密码加密功能
        String username = "testuser";
        String originalPassword = "mySecretPassword123!@#";
        boolean rememberPassword = true;

        // 保存凭据
        credentialManager.saveCredentials(username, originalPassword, rememberPassword);

        // 读取凭据
        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();

        // 验证密码正确解密
        assertNotNull("应该能读取到保存的凭据", savedCredentials);
        assertEquals("密码应该正确解密", originalPassword, savedCredentials.getPassword());
    }

    @Test
    public void testMultipleUsersCredentials() {
        // 测试多个用户凭据的覆盖行为
        String user1 = "user1";
        String password1 = "password1";
        String user2 = "user2";
        String password2 = "password2";

        // 保存第一个用户的凭据
        credentialManager.saveCredentials(user1, password1, true);
        CredentialManager.SavedCredentials saved1 = credentialManager.loadCredentials();
        assertEquals("应该保存第一个用户", user1, saved1.getUsername());

        // 保存第二个用户的凭据（应该覆盖第一个）
        credentialManager.saveCredentials(user2, password2, true);
        CredentialManager.SavedCredentials saved2 = credentialManager.loadCredentials();
        assertEquals("应该保存第二个用户", user2, saved2.getUsername());
        assertEquals("应该保存第二个用户的密码", password2, saved2.getPassword());
    }

    @Test
    public void testEmptyCredentials() {
        // 测试空凭据处理
        credentialManager.saveCredentials("", "", true);
        
        // 验证空凭据不会被保存
        assertFalse("空凭据不应该被保存", credentialManager.hasCredentials());

        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();
        assertNull("不应该能读取到空凭据", savedCredentials);
    }

    @Test
    public void testNullCredentials() {
        // 测试null凭据处理
        credentialManager.saveCredentials(null, null, true);
        
        // 验证null凭据不会被保存
        assertFalse("null凭据不应该被保存", credentialManager.hasCredentials());

        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();
        assertNull("不应该能读取到null凭据", savedCredentials);
    }

    @Test
    public void testSpecialCharactersInPassword() {
        // 测试包含特殊字符的密码
        String username = "testuser";
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~";
        boolean rememberPassword = true;

        // 保存包含特殊字符的密码
        credentialManager.saveCredentials(username, specialPassword, rememberPassword);

        // 读取并验证
        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();
        assertNotNull("应该能保存包含特殊字符的密码", savedCredentials);
        assertEquals("特殊字符密码应该正确解密", specialPassword, savedCredentials.getPassword());
    }

    @Test
    public void testChineseCharactersInCredentials() {
        // 测试包含中文字符的凭据
        String username = "测试用户";
        String password = "测试密码123";
        boolean rememberPassword = true;

        // 保存包含中文的凭据
        credentialManager.saveCredentials(username, password, rememberPassword);

        // 读取并验证
        CredentialManager.SavedCredentials savedCredentials = credentialManager.loadCredentials();
        assertNotNull("应该能保存包含中文的凭据", savedCredentials);
        assertEquals("中文用户名应该正确保存", username, savedCredentials.getUsername());
        assertEquals("中文密码应该正确解密", password, savedCredentials.getPassword());
    }
}
