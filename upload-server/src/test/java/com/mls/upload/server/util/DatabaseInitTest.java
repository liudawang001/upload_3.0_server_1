package com.mls.upload.server.util;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库初始化测试类
 * 用于创建测试用户和初始化数据
 */
public class DatabaseInitTest {

    private static final String DB_URL = "jdbc:mysql://47.110.46.27:3306/pic_system_mls?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "mls01";
    private static final String DB_PASSWORD = "12345@Mls";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 检查并创建admin用户
     */
    @Test
    public void createAdminUser() {
        Connection connection = null;
        PreparedStatement checkStatement = null;
        PreparedStatement insertStatement = null;
        PreparedStatement updateStatement = null;
        ResultSet resultSet = null;

        try {
            System.out.println("=== 开始创建admin用户 ===");
            
            // 建立数据库连接
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            System.out.println("数据库连接成功");

            // 检查用户是否已存在
            String checkSql = "SELECT id, username, nickname FROM pic_user WHERE username = ?";
            checkStatement = connection.prepareStatement(checkSql);
            checkStatement.setString(1, "admin");
            resultSet = checkStatement.executeQuery();

            String plainPassword = "admin123";
            String encodedPassword = passwordEncoder.encode(plainPassword);
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            if (resultSet.next()) {
                // 用户已存在，更新密码
                int userId = resultSet.getInt("id");
                String nickname = resultSet.getString("nickname");
                System.out.println("用户已存在: ID=" + userId + ", 昵称=" + nickname);
                
                String updateSql = "UPDATE pic_user SET password = ?, updata_time = ? WHERE username = ?";
                updateStatement = connection.prepareStatement(updateSql);
                updateStatement.setString(1, encodedPassword);
                updateStatement.setString(2, currentTime);
                updateStatement.setString(3, "admin");
                
                int updateResult = updateStatement.executeUpdate();
                if (updateResult > 0) {
                    System.out.println("✅ admin用户密码更新成功");
                    System.out.println("用户名: admin");
                    System.out.println("密码: " + plainPassword);
                    System.out.println("加密后: " + encodedPassword);
                } else {
                    System.out.println("❌ admin用户密码更新失败");
                }
            } else {
                // 用户不存在，创建新用户
                System.out.println("用户不存在，创建新用户");
                
                String insertSql = "INSERT INTO pic_user (username, password, nickname, create_time, updata_time) VALUES (?, ?, ?, ?, ?)";
                insertStatement = connection.prepareStatement(insertSql);
                insertStatement.setString(1, "admin");
                insertStatement.setString(2, encodedPassword);
                insertStatement.setString(3, "管理员");
                insertStatement.setString(4, currentTime);
                insertStatement.setString(5, currentTime);
                
                int insertResult = insertStatement.executeUpdate();
                if (insertResult > 0) {
                    System.out.println("✅ admin用户创建成功");
                    System.out.println("用户名: admin");
                    System.out.println("密码: " + plainPassword);
                    System.out.println("昵称: 管理员");
                    System.out.println("加密后: " + encodedPassword);
                } else {
                    System.out.println("❌ admin用户创建失败");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 数据库操作失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (checkStatement != null) checkStatement.close();
                if (insertStatement != null) insertStatement.close();
                if (updateStatement != null) updateStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }

    /**
     * 查看现有用户
     */
    @Test
    public void listExistingUsers() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            System.out.println("=== 查看现有用户 ===");
            
            // 建立数据库连接
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            // 查询所有用户
            String sql = "SELECT id, username, nickname, create_time FROM pic_user ORDER BY id";
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            
            System.out.println("现有用户列表:");
            System.out.println("ID\t用户名\t\t昵称\t\t创建时间");
            System.out.println("------------------------------------------------------------");
            
            while (resultSet.next()) {
                System.out.printf("%d\t%s\t\t%s\t\t%s%n",
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("nickname"),
                    resultSet.getTimestamp("create_time"));
            }

        } catch (Exception e) {
            System.err.println("❌ 查询用户失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }

    /**
     * 验证用户密码
     */
    @Test
    public void verifyUserPassword() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            System.out.println("=== 验证用户密码 ===");
            
            // 建立数据库连接
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            // 查询admin用户的密码
            String sql = "SELECT username, password FROM pic_user WHERE username = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, "admin");
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String storedPassword = resultSet.getString("password");
                
                System.out.println("用户名: " + username);
                System.out.println("存储的密码: " + storedPassword);
                
                // 验证密码
                String testPassword = "admin123";
                boolean matches = passwordEncoder.matches(testPassword, storedPassword);
                
                System.out.println("测试密码: " + testPassword);
                System.out.println("密码验证: " + (matches ? "✅ 成功" : "❌ 失败"));
                
            } else {
                System.out.println("❌ 未找到admin用户");
            }

        } catch (Exception e) {
            System.err.println("❌ 验证密码失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}
