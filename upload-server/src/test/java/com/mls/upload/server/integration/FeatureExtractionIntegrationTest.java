package com.mls.upload.server.integration;

import com.mls.upload.server.config.FeatureExtractionProperties;
import com.mls.upload.server.entity.DataVector;
import com.mls.upload.server.mapper.DataVectorMapper;
import com.mls.upload.server.service.DataService;
import com.mls.upload.server.service.DockerFeatureExtractionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Docker特征提取集成测试
 * 验证完整的特征提取流程
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "app.feature.extraction.docker.enabled=true",
    "app.feature.extraction.docker.url=http://test:5000/api/add_pic",
    "app.feature.extraction.docker.timeout=30000",
    "app.feature.extraction.docker.retry-times=2",
    "app.feature.extraction.docker.health-check-enabled=true"
})
public class FeatureExtractionIntegrationTest {

    @Autowired
    private FeatureExtractionProperties properties;

    @Autowired
    private DockerFeatureExtractionService dockerFeatureExtractionService;

    @Autowired
    private DataService dataService;

    @MockBean(name = "featureExtractionRestTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private DataVectorMapper dataVectorMapper;

    @Before
    public void setUp() {
        // 重置Mock对象
        reset(restTemplate, dataVectorMapper);
    }

    @Test
    public void testConfigurationLoading() {
        // 测试配置加载
        assertNotNull("配置属性应该被正确注入", properties);
        assertTrue("测试环境应该启用特征提取", properties.isEnabled());
        assertEquals("URL应该正确配置", "http://test:5000/api/add_pic", properties.getUrl());
        assertEquals("超时时间应该正确配置", 30000, properties.getTimeout());
        assertEquals("重试次数应该正确配置", 2, properties.getRetryTimes());
    }

    @Test
    public void testServiceInjection() {
        // 测试服务注入
        assertNotNull("DockerFeatureExtractionService应该被正确注入", dockerFeatureExtractionService);
        assertNotNull("DataService应该被正确注入", dataService);
    }

    @Test
    public void testHealthCheckIntegration() {
        // 测试健康检查集成
        
        // 模拟健康检查成功
        ResponseEntity<String> healthResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(healthResponse);
        
        boolean isAvailable = dockerFeatureExtractionService.isServiceAvailable();
        assertTrue("健康检查应该返回true", isAvailable);
        
        String healthStatus = dockerFeatureExtractionService.getHealthStatus();
        assertEquals("健康状态应该正常", "Docker特征提取服务正常", healthStatus);
    }

    @Test
    public void testFeatureExtractionFlow() throws IOException {
        // 测试完整的特征提取流程
        
        // 创建测试图片文件
        Path tempFile = Files.createTempFile("test-image", ".jpg");
        byte[] imageData = createTestImageData();
        Files.write(tempFile, imageData);
        
        try {
            // 模拟Docker服务响应
            Map<String, Object> dockerResponse = createMockDockerResponse();
            when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(dockerResponse, HttpStatus.OK));
            
            // 执行特征提取
            DockerFeatureExtractionService.MultiFeatureVector features = 
                dockerFeatureExtractionService.extractFeatures(tempFile.toFile());
            
            // 验证结果
            assertNotNull("特征提取应该成功", features);
            assertTrue("特征向量应该有效", features.isValid());
            
            // 验证各个特征向量的维度
            assertEquals("Color特征维度", 256, features.getColor().length);
            assertEquals("GLCM特征维度", 72, features.getGlcm().length);
            assertEquals("LBP特征维度", 256, features.getLbp().length);
            assertEquals("VGG特征维度", 512, features.getVgg().length);
            assertEquals("VIT特征维度", 768, features.getVit().length);
            
            // 验证特征值不为空
            assertNotNull("Color特征不应为null", features.getColor());
            assertNotNull("GLCM特征不应为null", features.getGlcm());
            assertNotNull("LBP特征不应为null", features.getLbp());
            assertNotNull("VGG特征不应为null", features.getVgg());
            assertNotNull("VIT特征不应为null", features.getVit());
            
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testDataServiceIntegration() {
        // 测试DataService集成
        
        // 模拟数据库查询：文件不存在
        when(dataVectorMapper.countByFilename("test.jpg")).thenReturn(0);
        
        // 模拟健康检查成功
        ResponseEntity<String> healthResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(healthResponse);
        
        // 模拟Docker服务响应
        Map<String, Object> dockerResponse = createMockDockerResponse();
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(dockerResponse, HttpStatus.OK));
        
        // 模拟数据保存成功
        when(dataVectorMapper.insert(any(DataVector.class))).thenReturn(1);
        
        // 执行特征提取请求
        boolean result = dataService.requestFeatureExtraction("test.jpg", "/path/to/test.jpg");
        
        assertTrue("特征提取请求应该成功", result);
        
        // 验证调用
        verify(dataVectorMapper).countByFilename("test.jpg");
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
    }

    @Test
    public void testErrorHandlingIntegration() {
        // 测试错误处理集成
        
        // 模拟数据库查询：文件不存在
        when(dataVectorMapper.countByFilename("error.jpg")).thenReturn(0);
        
        // 模拟健康检查失败
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));
        
        // 执行特征提取请求
        boolean result = dataService.requestFeatureExtraction("error.jpg", "/path/to/error.jpg");
        
        assertFalse("健康检查失败时应该返回false", result);
        
        // 验证调用
        verify(dataVectorMapper).countByFilename("error.jpg");
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
    }

    @Test
    public void testDuplicateFileHandling() {
        // 测试重复文件处理
        
        // 模拟数据库查询：文件已存在
        when(dataVectorMapper.countByFilename("existing.jpg")).thenReturn(1);
        
        // 执行特征提取请求
        boolean result = dataService.requestFeatureExtraction("existing.jpg", "/path/to/existing.jpg");
        
        assertTrue("已存在文件应该跳过提取并返回true", result);
        
        // 验证只调用了数据库查询，没有调用Docker服务
        verify(dataVectorMapper).countByFilename("existing.jpg");
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(Map.class));
    }

    @Test
    public void testServiceDisabledHandling() {
        // 测试服务禁用处理
        
        // 临时禁用服务
        properties.setEnabled(false);
        
        try {
            byte[] imageData = createTestImageData();
            DockerFeatureExtractionService.MultiFeatureVector result = 
                dockerFeatureExtractionService.extractFeatures(imageData, "disabled.jpg");
            
            assertNull("服务禁用时应该返回null", result);
            
            String healthStatus = dockerFeatureExtractionService.getHealthStatus();
            assertEquals("禁用时的健康状态", "Docker特征提取服务已禁用", healthStatus);
            
        } finally {
            // 恢复服务状态
            properties.setEnabled(true);
        }
    }

    /**
     * 创建测试图片数据
     */
    private byte[] createTestImageData() {
        // 创建简单的测试数据
        return "fake image data for testing".getBytes();
    }

    /**
     * 创建模拟的Docker服务响应
     */
    private Map<String, Object> createMockDockerResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        List<List<Double>> vectors = new ArrayList<>();
        vectors.add(createDoubleList(256)); // Color特征
        vectors.add(createDoubleList(72));  // GLCM特征
        vectors.add(createDoubleList(256)); // LBP特征
        vectors.add(createDoubleList(512)); // VGG特征
        vectors.add(createDoubleList(768)); // VIT特征
        
        data.put("vectors", vectors);
        response.put("data", data);
        
        return response;
    }

    /**
     * 创建指定长度的Double列表
     */
    private List<Double> createDoubleList(int size) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(Math.random());
        }
        return list;
    }
}
