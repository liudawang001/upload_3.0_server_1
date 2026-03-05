package com.mls.upload.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * 用户凭据管理器
 * 负责安全地保存和读取用户登录凭据
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class CredentialManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CredentialManager.class);
    
    // Preferences节点名称
    private static final String PREFS_NODE = "com.mls.upload.client.credentials";
    
    // 存储键名
    private static final String KEY_REMEMBER_PASSWORD = "rememberPassword";
    private static final String KEY_LAST_USERNAME = "lastUsername";
    private static final String KEY_ENCRYPTED_PASSWORD = "encryptedPassword";
    private static final String KEY_ENCRYPTION_KEY = "encryptionKey";
    
    // AES加密算法
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    private final Preferences preferences;
    
    /**
     * 构造函数
     */
    public CredentialManager() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
    }
    
    /**
     * 保存用户凭据
     * 
     * @param username 用户名
     * @param password 密码（明文）
     * @param rememberPassword 是否记住密码
     */
    public void saveCredentials(String username, String password, boolean rememberPassword) {
        try {
            logger.info("保存用户凭据: username={}, rememberPassword={}", username, rememberPassword);
            
            // 保存记住密码选项
            preferences.putBoolean(KEY_REMEMBER_PASSWORD, rememberPassword);
            
            if (rememberPassword && username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty()) {
                // 保存用户名
                preferences.put(KEY_LAST_USERNAME, username);
                
                // 加密并保存密码
                String encryptedPassword = encryptPassword(password);
                preferences.put(KEY_ENCRYPTED_PASSWORD, encryptedPassword);
                
                logger.info("用户凭据保存成功: username={}", username);
            } else {
                // 如果不记住密码，清除保存的凭据
                clearCredentials();
                logger.info("已清除保存的用户凭据");
            }
            
            // 立即同步到磁盘
            preferences.flush();
            
        } catch (Exception e) {
            logger.error("保存用户凭据失败: username={}, error={}", username, e.getMessage(), e);
        }
    }
    
    /**
     * 读取保存的用户凭据
     * 
     * @return 用户凭据对象，如果没有保存的凭据则返回null
     */
    public SavedCredentials loadCredentials() {
        try {
            boolean rememberPassword = preferences.getBoolean(KEY_REMEMBER_PASSWORD, false);
            
            if (!rememberPassword) {
                logger.debug("用户选择不记住密码");
                return null;
            }
            
            String username = preferences.get(KEY_LAST_USERNAME, null);
            String encryptedPassword = preferences.get(KEY_ENCRYPTED_PASSWORD, null);
            
            if (username == null || encryptedPassword == null) {
                logger.debug("没有找到保存的用户凭据");
                return null;
            }
            
            // 解密密码
            String password = decryptPassword(encryptedPassword);
            
            if (password == null) {
                logger.warn("密码解密失败，清除保存的凭据");
                clearCredentials();
                return null;
            }
            
            logger.info("成功读取保存的用户凭据: username={}", username);
            return new SavedCredentials(username, password, rememberPassword);
            
        } catch (Exception e) {
            logger.error("读取用户凭据失败: error={}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 清除保存的用户凭据
     */
    public void clearCredentials() {
        try {
            preferences.remove(KEY_LAST_USERNAME);
            preferences.remove(KEY_ENCRYPTED_PASSWORD);
            preferences.remove(KEY_ENCRYPTION_KEY);
            preferences.putBoolean(KEY_REMEMBER_PASSWORD, false);
            preferences.flush();
            
            logger.info("已清除所有保存的用户凭据");
            
        } catch (Exception e) {
            logger.error("清除用户凭据失败: error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否有保存的凭据
     * 
     * @return true如果有保存的凭据
     */
    public boolean hasCredentials() {
        boolean rememberPassword = preferences.getBoolean(KEY_REMEMBER_PASSWORD, false);
        String username = preferences.get(KEY_LAST_USERNAME, null);
        String encryptedPassword = preferences.get(KEY_ENCRYPTED_PASSWORD, null);
        
        return rememberPassword && username != null && encryptedPassword != null;
    }
    
    /**
     * 获取记住密码选项状态
     * 
     * @return true如果用户选择记住密码
     */
    public boolean isRememberPassword() {
        return preferences.getBoolean(KEY_REMEMBER_PASSWORD, false);
    }
    
    /**
     * 加密密码
     * 
     * @param password 明文密码
     * @return 加密后的密码（Base64编码）
     */
    private String encryptPassword(String password) throws Exception {
        // 获取或生成加密密钥
        SecretKey secretKey = getOrCreateSecretKey();
        
        // 创建加密器
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        // 加密密码
        byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        
        // 返回Base64编码的加密结果
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * 解密密码
     * 
     * @param encryptedPassword 加密的密码（Base64编码）
     * @return 明文密码
     */
    private String decryptPassword(String encryptedPassword) throws Exception {
        // 获取加密密钥
        SecretKey secretKey = getOrCreateSecretKey();
        
        // 创建解密器
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        // 解码Base64并解密
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPassword);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        
        // 返回明文密码
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 获取或创建加密密钥
     * 
     * @return 加密密钥
     */
    private SecretKey getOrCreateSecretKey() throws Exception {
        String keyString = preferences.get(KEY_ENCRYPTION_KEY, null);
        
        if (keyString == null) {
            // 生成新的密钥
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(128, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            
            // 保存密钥
            keyString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            preferences.put(KEY_ENCRYPTION_KEY, keyString);
            preferences.flush();
            
            logger.debug("生成新的加密密钥");
            return secretKey;
        } else {
            // 使用现有密钥
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        }
    }
    
    /**
     * 保存的用户凭据数据类
     */
    public static class SavedCredentials {
        private final String username;
        private final String password;
        private final boolean rememberPassword;
        
        public SavedCredentials(String username, String password, boolean rememberPassword) {
            this.username = username;
            this.password = password;
            this.rememberPassword = rememberPassword;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public boolean isRememberPassword() {
            return rememberPassword;
        }
        
        @Override
        public String toString() {
            return "SavedCredentials{" +
                   "username='" + username + '\'' +
                   ", password='[PROTECTED]'" +
                   ", rememberPassword=" + rememberPassword +
                   '}';
        }
    }
}
