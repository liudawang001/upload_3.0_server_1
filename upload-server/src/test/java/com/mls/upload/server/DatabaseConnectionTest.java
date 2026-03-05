package com.mls.upload.server;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库连接测试类
 * 验证数据库连接配置和表结构
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class DatabaseConnectionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);
    
    // 数据库连接配置
    private static final String DB_URL = "jdbc:mysql://47.110.46.27:3306/pic_system_mls?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String DB_USERNAME = "mls01";
    private static final String DB_PASSWORD = "12345@Mls";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    /**
     * 测试数据库连接
     */
    @Test
    public void testDatabaseConnection() {
        Connection connection = null;
        try {
            logger.info("开始测试数据库连接...");
            
            // 加载数据库驱动
            Class.forName(DB_DRIVER);
            logger.info("数据库驱动加载成功: {}", DB_DRIVER);
            
            // 建立数据库连接
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            logger.info("数据库连接建立成功");
            
            // 测试连接是否有效
            if (connection != null && !connection.isClosed()) {
                logger.info("数据库连接状态: 正常");
                logger.info("数据库产品名称: {}", connection.getMetaData().getDatabaseProductName());
                logger.info("数据库版本: {}", connection.getMetaData().getDatabaseProductVersion());
                logger.info("驱动版本: {}", connection.getMetaData().getDriverVersion());
            } else {
                logger.error("数据库连接状态: 异常");
            }
            
        } catch (ClassNotFoundException e) {
            logger.error("数据库驱动加载失败", e);
        } catch (SQLException e) {
            logger.error("数据库连接失败", e);
        } finally {
            // 关闭连接
            if (connection != null) {
                try {
                    connection.close();
                    logger.info("数据库连接已关闭");
                } catch (SQLException e) {
                    logger.error("关闭数据库连接时发生错误", e);
                }
            }
        }
    }
    
    /**
     * 测试数据库表结构
     */
    @Test
    public void testDatabaseTables() {
        Connection connection = null;
        try {
            logger.info("开始测试数据库表结构...");
            
            // 建立数据库连接
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            // 测试pic_user表
            testTable(connection, "pic_user", "用户表");
            
            // 测试image_upload表
            testTable(connection, "image_upload", "图片上传记录表");
            
            // 测试pic_info表
            testTable(connection, "pic_info", "图片信息表");
            
            // 测试data_vector表
            testTable(connection, "data_vector", "特征向量表");
            
            logger.info("数据库表结构测试完成");
            
        } catch (Exception e) {
            logger.error("数据库表结构测试失败", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接时发生错误", e);
                }
            }
        }
    }
    
    /**
     * 测试指定表是否存在
     * 
     * @param connection 数据库连接
     * @param tableName 表名
     * @param description 表描述
     */
    private void testTable(Connection connection, String tableName, String description) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'pic_system_mls' AND table_name = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, tableName);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                if (count > 0) {
                    logger.info("表 {} ({}) 存在", tableName, description);
                    
                    // 查询表的记录数
                    testTableRecordCount(connection, tableName, description);
                } else {
                    logger.warn("表 {} ({}) 不存在", tableName, description);
                }
            }
            
        } catch (SQLException e) {
            logger.error("测试表 {} 时发生错误", tableName, e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.error("关闭资源时发生错误", e);
            }
        }
    }
    
    /**
     * 测试表的记录数
     * 
     * @param connection 数据库连接
     * @param tableName 表名
     * @param description 表描述
     */
    private void testTableRecordCount(Connection connection, String tableName, String description) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT COUNT(*) as count FROM " + tableName;
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                logger.info("表 {} ({}) 当前记录数: {}", tableName, description, count);
            }
            
        } catch (SQLException e) {
            logger.error("查询表 {} 记录数时发生错误", tableName, e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                logger.error("关闭资源时发生错误", e);
            }
        }
    }
    
    /**
     * 测试用户表数据
     */
    @Test
    public void testUserTableData() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            logger.info("开始测试用户表数据...");
            
            // 建立数据库连接
            Class.forName(DB_DRIVER);
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            // 查询用户表数据
            String sql = "SELECT id, username, nickname, create_time FROM pic_user LIMIT 5";
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            
            logger.info("用户表前5条记录:");
            while (resultSet.next()) {
                logger.info("ID: {}, 用户名: {}, 昵称: {}, 创建时间: {}", 
                    resultSet.getInt("id"),
                    resultSet.getString("username"),
                    resultSet.getString("nickname"),
                    resultSet.getTimestamp("create_time"));
            }
            
        } catch (Exception e) {
            logger.error("测试用户表数据失败", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.error("关闭资源时发生错误", e);
            }
        }
    }
}
