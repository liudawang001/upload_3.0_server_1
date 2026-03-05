package com.mls.upload.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Docker特征提取服务配置属性类
 * 映射application.yml中的app.feature.extraction.docker配置项
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "app.feature.extraction.docker")
@Validated
public class FeatureExtractionProperties {

    /**
     * 是否启用特征提取功能
     * 默认为false，确保系统稳定性
     */
    private boolean enabled = false;

    /**
     * Docker特征提取服务URL
     * 根据Docker特征提取方法参考.md，使用/api/add_pic端点
     */
    @NotBlank(message = "Docker服务URL不能为空")
    private String url = "http://192.168.0.79:5000/api/add_pic";

    /**
     * 请求超时时间（毫秒）
     * 包含连接建立、数据传输和响应处理的总时间
     */
    @Min(value = 1000, message = "超时时间不能少于1秒")
    private int timeout = 60000;

    /**
     * 连接超时时间（毫秒）
     * Docker服务连接建立的最大等待时间
     */
    @Min(value = 1000, message = "连接超时时间不能少于1秒")
    private int connectTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     * 从Docker服务读取响应数据的最大等待时间
     */
    @Min(value = 1000, message = "读取超时时间不能少于1秒")
    private int readTimeout = 60000;

    /**
     * 重试次数
     * 当Docker服务调用失败时的重试次数
     */
    @Min(value = 0, message = "重试次数不能为负数")
    private int retryTimes = 3;

    /**
     * 重试间隔时间（毫秒）
     * 两次重试之间的等待时间
     */
    @Min(value = 100, message = "重试间隔时间不能少于100毫秒")
    private int retryInterval = 1000;

    /**
     * 健康检查URL
     * 用于检测Docker服务是否可用
     */
    private String healthCheckUrl = "http://192.168.0.79:5000/health";

    /**
     * 是否启用健康检查
     * 在调用特征提取前检查服务可用性
     */
    private boolean healthCheckEnabled = true;

    /**
     * 是否覆盖已存在的特征向量
     * true: 重新提取并覆盖已存在的特征向量
     * false: 跳过已存在特征向量的文件（默认行为，保持向后兼容）
     */
    private boolean overwriteExisting = false;

    // Getter和Setter方法

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public void setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    @Override
    public String toString() {
        return "FeatureExtractionProperties{" +
                "enabled=" + enabled +
                ", url='" + url + '\'' +
                ", timeout=" + timeout +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                ", retryTimes=" + retryTimes +
                ", retryInterval=" + retryInterval +
                ", healthCheckUrl='" + healthCheckUrl + '\'' +
                ", healthCheckEnabled=" + healthCheckEnabled +
                ", overwriteExisting=" + overwriteExisting +
                '}';
    }
}
