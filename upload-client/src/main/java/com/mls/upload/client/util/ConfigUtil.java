package com.mls.upload.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

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
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_SYSTEM_PROPERTY = "mls.upload.client.config";
    private static final String CONFIG_ENV = "MLS_UPLOAD_CLIENT_CONFIG";
    private static Properties properties;
    
    static {
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        properties = new Properties();

        loadDefaultConfig();
        loadClasspathConfig(CONFIG_FILE);
        loadExternalConfigs();
        normalizeServerConfig();
    }

    private static void loadClasspathConfig(String configFile) {
        try (InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream(configFile)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.info("内置配置文件加载成功: {}", configFile);
            } else {
                logger.warn("内置配置文件未找到: {}", configFile);
            }
        } catch (IOException e) {
            logger.error("内置配置文件加载失败: {}", e.getMessage(), e);
        }
    }

    private static void loadExternalConfigs() {
        Set<File> candidates = new LinkedHashSet<>();

        addConfigCandidate(candidates, new File(System.getProperty("user.dir"), CONFIG_FILE));
        addConfigCandidate(candidates, new File(new File(System.getProperty("user.dir"), CONFIG_DIR), CONFIG_FILE));

        File applicationDir = getApplicationDir();
        if (applicationDir != null) {
            addConfigCandidate(candidates, new File(applicationDir, CONFIG_FILE));
            addConfigCandidate(candidates, new File(new File(applicationDir, CONFIG_DIR), CONFIG_FILE));
        }

        String envConfigPath = System.getenv(CONFIG_ENV);
        if (envConfigPath != null && !envConfigPath.trim().isEmpty()) {
            addConfigCandidate(candidates, new File(envConfigPath.trim()));
        }

        String systemConfigPath = System.getProperty(CONFIG_SYSTEM_PROPERTY);
        if (systemConfigPath != null && !systemConfigPath.trim().isEmpty()) {
            addConfigCandidate(candidates, new File(systemConfigPath.trim()));
        }

        for (File candidate : candidates) {
            if (!candidate.exists() || !candidate.isFile()) {
                continue;
            }

            try (InputStream inputStream = new FileInputStream(candidate)) {
                properties.load(inputStream);
                logger.info("外置配置文件加载成功: {}", candidate.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("外置配置文件加载失败: {}, error={}",
                           candidate.getAbsolutePath(), e.getMessage());
            }
        }
    }

    private static void addConfigCandidate(Set<File> candidates, File file) {
        if (file == null) {
            return;
        }

        try {
            candidates.add(file.getCanonicalFile());
        } catch (IOException e) {
            candidates.add(file.getAbsoluteFile());
        }
    }

    private static File getApplicationDir() {
        try {
            File codeSource = new File(ConfigUtil.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (codeSource.isFile()) {
                return codeSource.getParentFile();
            }
            return codeSource;
        } catch (URISyntaxException | SecurityException e) {
            logger.debug("无法解析程序所在目录: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载默认配置
     */
    private static void loadDefaultConfig() {
        properties.setProperty("server.url", "http://192.168.30.114:8081");
        properties.setProperty("server.timeout", "30000");
        properties.setProperty("upload.chunk.size", "1048576");
        properties.setProperty("upload.thread.pool.size", "5");
        properties.setProperty("upload.feature.extraction.enabled", "true");
        properties.setProperty("log.level", "INFO");
        logger.debug("默认配置加载完成");
    }
    
    /**
     * 获取服务器URL
     * 
     * @return 服务器URL
     */
    public static String getServerUrl() {
        String serverUrl = properties.getProperty("server.url");
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            serverUrl = properties.getProperty("server.base.url");
        }

        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            String protocol = properties.getProperty("server.protocol", "http");
            String host = properties.getProperty("server.host", "192.168.30.114");
            String port = properties.getProperty("server.port", "8081");
            serverUrl = protocol + "://" + host + ":" + port;
        }

        return normalizeServerUrl(resolvePlaceholders(serverUrl));
    }

    /**
     * 是否在上传花型图后触发服务端特征提取。
     */
    public static boolean isUploadFeatureExtractionEnabled() {
        return getBooleanProperty("upload.feature.extraction.enabled", true);
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
        String value = properties.getProperty(key);
        return value == null ? null : resolvePlaceholders(value);
    }
    
    /**
     * 获取配置属性值（带默认值）
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getProperty(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        return value == null ? null : resolvePlaceholders(value);
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
        String value = getProperty(key);
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
        String value = getProperty(key);
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
        String value = getProperty(key);
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

    private static void normalizeServerConfig() {
        String serverUrl = getServerUrl();
        properties.setProperty("server.url", serverUrl);
        logger.info("客户端服务端地址: {}", serverUrl);
    }

    private static String normalizeServerUrl(String url) {
        if (url == null) {
            return "http://192.168.30.114:8081";
        }

        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/api")) {
            normalized = normalized.substring(0, normalized.length() - 4);
            logger.warn("server.url不应包含/api，已自动修正为: {}", normalized);
        }

        return normalized;
    }

    private static String resolvePlaceholders(String value) {
        if (value == null || value.indexOf("${") < 0) {
            return value;
        }

        String resolved = value;
        for (int i = 0; i < 5 && resolved.indexOf("${") >= 0; i++) {
            int start = resolved.indexOf("${");
            int end = resolved.indexOf("}", start);
            if (end <= start) {
                break;
            }

            String key = resolved.substring(start + 2, end);
            String replacement = properties.getProperty(key);
            if (replacement == null) {
                replacement = System.getenv(key);
            }
            if (replacement == null) {
                logger.warn("配置占位符未解析: {}", key);
                break;
            }

            resolved = resolved.substring(0, start) + replacement + resolved.substring(end + 1);
        }

        return resolved;
    }
}
