package com.mls.upload.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 配置工具类
 * 负责读取和管理应用程序配置信息
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ConfigUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private static final String CONFIG_FILE = "application.properties";
    private static Properties properties;
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        properties = new Properties();
        
        try (InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.info("配置文件加载成功: {}", CONFIG_FILE);
            } else {
                logger.warn("配置文件未找到: {}，使用默认配置", CONFIG_FILE);
                loadDefaultConfig();
            }
        } catch (IOException e) {
            logger.error("配置文件加载失败: {}", e.getMessage(), e);
            loadDefaultConfig();
        }
    }
    
    /**
     * 加载默认配置
     */
    private static void loadDefaultConfig() {
        properties.setProperty("server.url", "http://192.168.0.79:8081");
        properties.setProperty("server.timeout", "30000");
        properties.setProperty("upload.chunk.size", "1048576");
        properties.setProperty("upload.thread.pool.size", "5");
        properties.setProperty("log.level", "INFO");
        logger.info("默认配置加载完成");
    }
    
    /**
     * 获取服务器URL
     * 
     * @return 服务器URL
     */
    public static String getServerUrl() {
        return properties.getProperty("server.url", "http://192.168.0.79:8081");
    }
    
    /**
     * 获取服务器超时时间
     * 
     * @return 超时时间（毫秒）
     */
    public static int getServerTimeout() {
        String timeout = properties.getProperty("server.timeout", "30000");
        try {
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            logger.warn("服务器超时时间配置无效: {}，使用默认值30000", timeout);
            return 30000;
        }
    }
    
    /**
     * 获取上传分块大小
     * 
     * @return 分块大小（字节）
     */
    public static int getUploadChunkSize() {
        String chunkSize = properties.getProperty("upload.chunk.size", "1048576");
        try {
            return Integer.parseInt(chunkSize);
        } catch (NumberFormatException e) {
            logger.warn("上传分块大小配置无效: {}，使用默认值1048576", chunkSize);
            return 1048576; // 1MB
        }
    }
    
    /**
     * 获取上传线程池大小
     * 
     * @return 线程池大小
     */
    public static int getUploadThreadPoolSize() {
        String poolSize = properties.getProperty("upload.thread.pool.size", "5");
        try {
            return Integer.parseInt(poolSize);
        } catch (NumberFormatException e) {
            logger.warn("上传线程池大小配置无效: {}，使用默认值5", poolSize);
            return 5;
        }
    }
    
    /**
     * 获取日志级别
     * 
     * @return 日志级别
     */
    public static String getLogLevel() {
        return properties.getProperty("log.level", "INFO");
    }
    
    /**
     * 获取配置属性值
     * 
     * @param key 配置键
     * @return 配置值
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * 获取配置属性值（带默认值）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 设置配置属性值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
        logger.debug("配置属性已更新: {}={}", key, value);
    }
    
    /**
     * 获取所有配置属性
     * 
     * @return 配置属性对象
     */
    public static Properties getAllProperties() {
        return new Properties(properties);
    }
    
    /**
     * 重新加载配置文件
     */
    public static void reloadConfig() {
        logger.info("重新加载配置文件");
        loadConfig();
    }
    
    /**
     * 检查配置是否存在
     * 
     * @param key 配置键
     * @return 是否存在
     */
    public static boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * 获取布尔类型配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 获取整数类型配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 整数值
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("配置值转换为整数失败: {}={}，使用默认值{}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取长整数类型配置值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 长整数值
     */
    public static long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("配置值转换为长整数失败: {}={}，使用默认值{}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
