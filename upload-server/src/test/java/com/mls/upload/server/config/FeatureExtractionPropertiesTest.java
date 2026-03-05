package com.mls.upload.server.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * 特征提取配置属性测试
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FeatureExtractionPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
    "app.feature.extraction.docker.enabled=true",
    "app.feature.extraction.docker.url=http://test:5000/api/add_pic",
    "app.feature.extraction.docker.timeout=30000",
    "app.feature.extraction.docker.connect-timeout=15000",
    "app.feature.extraction.docker.read-timeout=45000",
    "app.feature.extraction.docker.retry-times=5",
    "app.feature.extraction.docker.retry-interval=2000",
    "app.feature.extraction.docker.health-check-url=http://test:5000/health",
    "app.feature.extraction.docker.health-check-enabled=false"
})
public class FeatureExtractionPropertiesTest {

    @Autowired
    private FeatureExtractionProperties properties;

    @Test
    public void testConfigurationPropertiesBinding() {
        // 测试配置属性绑定
        assertTrue("enabled应该为true", properties.isEnabled());
        assertEquals("URL应该正确绑定", "http://test:5000/api/add_pic", properties.getUrl());
        assertEquals("timeout应该正确绑定", 30000, properties.getTimeout());
        assertEquals("connectTimeout应该正确绑定", 15000, properties.getConnectTimeout());
        assertEquals("readTimeout应该正确绑定", 45000, properties.getReadTimeout());
        assertEquals("retryTimes应该正确绑定", 5, properties.getRetryTimes());
        assertEquals("retryInterval应该正确绑定", 2000, properties.getRetryInterval());
        assertEquals("healthCheckUrl应该正确绑定", "http://test:5000/health", properties.getHealthCheckUrl());
        assertFalse("healthCheckEnabled应该为false", properties.isHealthCheckEnabled());
    }

    @Test
    public void testDefaultValues() {
        // 测试默认值
        FeatureExtractionProperties defaultProps = new FeatureExtractionProperties();
        
        assertFalse("默认enabled应该为false", defaultProps.isEnabled());
        assertEquals("默认URL", "http://192.168.1.78:5000/api/add_pic", defaultProps.getUrl());
        assertEquals("默认timeout", 60000, defaultProps.getTimeout());
        assertEquals("默认connectTimeout", 30000, defaultProps.getConnectTimeout());
        assertEquals("默认readTimeout", 60000, defaultProps.getReadTimeout());
        assertEquals("默认retryTimes", 3, defaultProps.getRetryTimes());
        assertEquals("默认retryInterval", 1000, defaultProps.getRetryInterval());
        assertEquals("默认healthCheckUrl", "http://192.168.1.78:5000/health", defaultProps.getHealthCheckUrl());
        assertTrue("默认healthCheckEnabled应该为true", defaultProps.isHealthCheckEnabled());
    }

    @Test
    public void testToString() {
        // 测试toString方法
        String toString = properties.toString();
        
        assertNotNull("toString不应该为null", toString);
        assertTrue("toString应该包含类名", toString.contains("FeatureExtractionProperties"));
        assertTrue("toString应该包含enabled", toString.contains("enabled=true"));
        assertTrue("toString应该包含url", toString.contains("url='http://test:5000/api/add_pic'"));
    }

    @Test
    public void testSettersAndGetters() {
        // 测试setter和getter方法
        FeatureExtractionProperties testProps = new FeatureExtractionProperties();
        
        testProps.setEnabled(true);
        assertTrue("setEnabled应该生效", testProps.isEnabled());
        
        testProps.setUrl("http://custom:8080/api");
        assertEquals("setUrl应该生效", "http://custom:8080/api", testProps.getUrl());
        
        testProps.setTimeout(45000);
        assertEquals("setTimeout应该生效", 45000, testProps.getTimeout());
        
        testProps.setConnectTimeout(20000);
        assertEquals("setConnectTimeout应该生效", 20000, testProps.getConnectTimeout());
        
        testProps.setReadTimeout(50000);
        assertEquals("setReadTimeout应该生效", 50000, testProps.getReadTimeout());
        
        testProps.setRetryTimes(5);
        assertEquals("setRetryTimes应该生效", 5, testProps.getRetryTimes());
        
        testProps.setRetryInterval(1500);
        assertEquals("setRetryInterval应该生效", 1500, testProps.getRetryInterval());
        
        testProps.setHealthCheckUrl("http://custom:8080/health");
        assertEquals("setHealthCheckUrl应该生效", "http://custom:8080/health", testProps.getHealthCheckUrl());
        
        testProps.setHealthCheckEnabled(false);
        assertFalse("setHealthCheckEnabled应该生效", testProps.isHealthCheckEnabled());
    }

    /**
     * 测试配置类
     */
    @EnableConfigurationProperties(FeatureExtractionProperties.class)
    static class TestConfig {
        // 空配置类，仅用于启用配置属性
    }
}
