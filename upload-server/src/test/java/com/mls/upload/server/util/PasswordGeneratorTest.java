package com.mls.upload.server.util;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码生成工具测试类
 * 用于生成BCrypt加密密码和验证密码
 */
public class PasswordGeneratorTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 生成admin123的BCrypt密码
     */
    @Test
    public void generateAdminPassword() {
        String plainPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        
        System.out.println("=== 密码生成结果 ===");
        System.out.println("明文密码: " + plainPassword);
        System.out.println("加密密码: " + encodedPassword);
        System.out.println("密码长度: " + encodedPassword.length());
        
        // 验证密码是否正确
        boolean matches = passwordEncoder.matches(plainPassword, encodedPassword);
        System.out.println("密码验证: " + (matches ? "成功" : "失败"));
    }

    /**
     * 生成多个常用密码的BCrypt加密结果
     */
    @Test
    public void generateCommonPasswords() {
        String[] passwords = {"admin", "admin123", "123456", "password", "test123"};
        
        System.out.println("=== 常用密码加密结果 ===");
        for (String password : passwords) {
            String encoded = passwordEncoder.encode(password);
            System.out.println("明文: " + password + " -> 加密: " + encoded);
        }
    }

    /**
     * 验证现有密码
     */
    @Test
    public void verifyExistingPassword() {
        // 如果你知道数据库中的加密密码，可以在这里验证
        String plainPassword = "admin123";
        String existingEncodedPassword = "$2a$10$example"; // 替换为实际的加密密码
        
        boolean matches = passwordEncoder.matches(plainPassword, existingEncodedPassword);
        System.out.println("密码验证结果: " + (matches ? "匹配" : "不匹配"));
    }

    /**
     * 生成SQL插入语句
     */
    @Test
    public void generateInsertSQL() {
        String username = "admin";
        String plainPassword = "admin123";
        String nickname = "管理员";
        String encodedPassword = passwordEncoder.encode(plainPassword);
        
        System.out.println("=== SQL插入语句 ===");
        System.out.println("INSERT INTO pic_user (username, password, nickname, create_time, updata_time) VALUES");
        System.out.println("('" + username + "', '" + encodedPassword + "', '" + nickname + "', NOW(), NOW());");
        
        System.out.println("\n=== 更新语句（如果用户已存在）===");
        System.out.println("UPDATE pic_user SET password = '" + encodedPassword + "', updata_time = NOW() WHERE username = '" + username + "';");
    }
}
