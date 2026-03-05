package com.mls.upload.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 为Docker特征提取服务提供专用的HTTP客户端配置
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Autowired
    private FeatureExtractionProperties featureExtractionProperties;

    /**
     * 配置用于Docker特征提取服务的RestTemplate
     * 根据Docker特征提取方法参考.md中的配置要求进行优化
     * 
     * @return 配置好的RestTemplate实例
     */
    @Bean(name = "featureExtractionRestTemplate")
    public RestTemplate featureExtractionRestTemplate() {
        logger.info("正在配置Docker特征提取服务专用RestTemplate...");

        // 创建HTTP请求工厂
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 设置连接超时时间
        factory.setConnectTimeout(featureExtractionProperties.getConnectTimeout());
        logger.debug("设置连接超时时间: {}ms", featureExtractionProperties.getConnectTimeout());
        
        // 设置读取超时时间
        factory.setReadTimeout(featureExtractionProperties.getReadTimeout());
        logger.debug("设置读取超时时间: {}ms", featureExtractionProperties.getReadTimeout());
        
        // 创建RestTemplate实例
        RestTemplate restTemplate = new RestTemplate(factory);
        
        logger.info("Docker特征提取服务RestTemplate配置完成");
        logger.info("连接超时: {}ms, 读取超时: {}ms", 
                   featureExtractionProperties.getConnectTimeout(),
                   featureExtractionProperties.getReadTimeout());
        
        return restTemplate;
    }

    /**
     * 配置缓存刷新专用的RestTemplate
     * 用于调用项目B的缓存更新API
     *
     * @param cacheRefreshProperties 缓存刷新配置属性
     * @return 缓存刷新RestTemplate实例
     */
    @Bean(name = "cacheRefreshRestTemplate")
    public RestTemplate cacheRefreshRestTemplate(CacheRefreshProperties cacheRefreshProperties) {
        logger.info("正在配置缓存刷新RestTemplate...");

        // 创建HTTP请求工厂
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置超时时间
        factory.setConnectTimeout(cacheRefreshProperties.getTimeout().getConnect());
        factory.setReadTimeout(cacheRefreshProperties.getTimeout().getRead());

        RestTemplate restTemplate = new RestTemplate(factory);

        logger.info("缓存刷新RestTemplate配置完成");
        logger.info("连接超时: {}ms, 读取超时: {}ms",
                   cacheRefreshProperties.getTimeout().getConnect(),
                   cacheRefreshProperties.getTimeout().getRead());

        return restTemplate;
    }

    /**
     * 配置通用的RestTemplate（保持向后兼容）
     * 为其他服务提供默认的HTTP客户端
     *
     * @return 通用RestTemplate实例
     */
    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        logger.info("正在配置通用RestTemplate...");

        // 创建HTTP请求工厂，使用默认配置
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置默认超时时间
        factory.setConnectTimeout(30000);  // 30秒连接超时
        factory.setReadTimeout(60000);     // 60秒读取超时

        RestTemplate restTemplate = new RestTemplate(factory);

        logger.info("通用RestTemplate配置完成");

        return restTemplate;
    }
}
