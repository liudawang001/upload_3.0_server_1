package com.mls.upload.server.config;

import com.mls.upload.server.service.DockerFeatureExtractionService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * 特征向量覆盖配置测试
 * 验证配置属性的正确加载和切换功能
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@SpringBootTest
public class FeatureExtractionOverwriteConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(FeatureExtractionOverwriteConfigTest.class);

    @Autowired
    private FeatureExtractionProperties properties;

    @Autowired
    private DockerFeatureExtractionService dockerFeatureExtractionService;

    @Test
    public void testDefaultOverwriteConfiguration() {
        // 验证默认配置
        assertNotNull("FeatureExtractionProperties应该被正确注入", properties);
        assertFalse("默认情况下应该禁用覆盖功能", properties.isOverwriteExisting());
        assertFalse("服务层应该反映配置状态", dockerFeatureExtractionService.isOverwriteEnabled());

        logger.info("✅ 默认覆盖配置测试通过: overwriteExisting={}", properties.isOverwriteExisting());
    }

    @Test
    public void testOverwriteConfigurationProperties() {
        // 验证配置属性的完整性
        assertNotNull("toString方法应该包含所有属性", properties.toString());
        assertTrue("toString应该包含overwriteExisting属性", properties.toString().contains("overwriteExisting"));

        // 验证getter和setter方法
        boolean originalValue = properties.isOverwriteExisting();
        properties.setOverwriteExisting(!originalValue);
        assertEquals("setter和getter应该正常工作", !originalValue, properties.isOverwriteExisting());

        // 恢复原始值
        properties.setOverwriteExisting(originalValue);
        assertEquals("配置值应该被正确恢复", originalValue, properties.isOverwriteExisting());

        logger.info("✅ 覆盖配置属性测试通过");
    }



    @Test
    public void testConfigurationConsistency() {
        // 验证配置在不同层级的一致性
        boolean propertiesValue = properties.isOverwriteExisting();
        boolean serviceValue = dockerFeatureExtractionService.isOverwriteEnabled();

        assertEquals("配置属性和服务层的值应该保持一致", propertiesValue, serviceValue);

        logger.info("✅ 配置一致性测试通过: properties={}, service={}", propertiesValue, serviceValue);
    }

    @Test
    public void testConfigurationValidation() {
        // 验证配置的有效性
        // Boolean类型不会为null，所以测试基本功能

        // 测试配置的边界值
        properties.setOverwriteExisting(true);
        assertTrue("设置为true时应该返回true", properties.isOverwriteExisting());

        properties.setOverwriteExisting(false);
        assertFalse("设置为false时应该返回false", properties.isOverwriteExisting());

        logger.info("✅ 配置验证测试通过");
    }

    @Test
    public void testBackwardCompatibility() {
        // 验证向后兼容性
        // 默认配置应该保持现有行为（不覆盖）
        assertFalse("默认配置应该保持向后兼容（不覆盖）", properties.isOverwriteExisting());

        // 验证现有功能不受影响
        assertNotNull("现有配置项应该正常工作", properties.getUrl());
        assertNotNull("现有配置项应该正常工作", properties.isEnabled());
        assertNotNull("现有配置项应该正常工作", properties.isHealthCheckEnabled());

        logger.info("✅ 向后兼容性测试通过");
    }
}
