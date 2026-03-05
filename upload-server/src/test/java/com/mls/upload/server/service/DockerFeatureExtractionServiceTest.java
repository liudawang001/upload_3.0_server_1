package com.mls.upload.server.service;

import com.mls.upload.server.config.FeatureExtractionProperties;
import com.mls.upload.server.service.impl.DockerFeatureExtractionServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Docker特征提取服务单元测试
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerFeatureExtractionServiceTest {

    @Mock
    private FeatureExtractionProperties properties;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DataService dataService;

    @InjectMocks
    private DockerFeatureExtractionServiceImpl dockerFeatureExtractionService;

    @Before
    public void setUp() {
        // 设置默认配置
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isHealthCheckEnabled()).thenReturn(true);
        when(properties.getHealthCheckUrl()).thenReturn("http://test:5000/health");
        when(properties.getUrl()).thenReturn("http://test:5000/api/add_pic");
        when(properties.getRetryTimes()).thenReturn(3);
        when(properties.getRetryInterval()).thenReturn(1000);
    }

    @Test
    public void testIsServiceAvailable_WhenDisabled() {
        // 测试服务禁用时的情况
        when(properties.isEnabled()).thenReturn(false);
        
        boolean result = dockerFeatureExtractionService.isServiceAvailable();
        
        assertFalse("服务禁用时应返回false", result);
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
    }

    @Test
    public void testIsServiceAvailable_WhenHealthCheckDisabled() {
        // 测试健康检查禁用时的情况
        when(properties.isHealthCheckEnabled()).thenReturn(false);
        
        boolean result = dockerFeatureExtractionService.isServiceAvailable();
        
        assertTrue("健康检查禁用时应假设服务可用", result);
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
    }

    @Test
    public void testIsServiceAvailable_WhenHealthy() {
        // 测试服务健康时的情况
        ResponseEntity<String> healthResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(healthResponse);
        
        boolean result = dockerFeatureExtractionService.isServiceAvailable();
        
        assertTrue("服务健康时应返回true", result);
        verify(restTemplate).getForEntity("http://test:5000/health", String.class);
    }

    @Test
    public void testIsServiceAvailable_WhenUnhealthy() {
        // 测试服务不健康时的情况
        ResponseEntity<String> healthResponse = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(healthResponse);
        
        boolean result = dockerFeatureExtractionService.isServiceAvailable();
        
        assertFalse("服务不健康时应返回false", result);
    }

    @Test
    public void testIsServiceAvailable_WhenException() {
        // 测试健康检查异常时的情况
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));
        
        boolean result = dockerFeatureExtractionService.isServiceAvailable();
        
        assertFalse("健康检查异常时应返回false", result);
    }

    @Test
    public void testExtractFeatures_WithInvalidFile() {
        // 测试无效文件的情况
        try {
            dockerFeatureExtractionService.extractFeatures((File) null);
            fail("应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("异常消息应包含文件无效信息", e.getMessage().contains("图片文件无效"));
        }
    }

    @Test
    public void testExtractFeatures_WithInvalidBytes() {
        // 测试无效字节数组的情况
        try {
            dockerFeatureExtractionService.extractFeatures(null, "test.jpg");
            fail("应该抛出IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("异常消息应包含数据为空信息", e.getMessage().contains("图片数据为空"));
        }
    }

    @Test
    public void testExtractFeatures_WhenServiceDisabled() {
        // 测试服务禁用时的情况
        when(properties.isEnabled()).thenReturn(false);
        
        byte[] imageBytes = "fake image data".getBytes();
        DockerFeatureExtractionService.MultiFeatureVector result = 
            dockerFeatureExtractionService.extractFeatures(imageBytes, "test.jpg");
        
        assertNull("服务禁用时应返回null", result);
    }

    @Test
    public void testExtractFeatures_Success() {
        // 测试成功提取特征的情况
        byte[] imageBytes = "fake image data".getBytes();
        
        // 模拟Docker服务响应
        Map<String, Object> dockerResponse = createMockDockerResponse();
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(dockerResponse, HttpStatus.OK));
        
        DockerFeatureExtractionService.MultiFeatureVector result = 
            dockerFeatureExtractionService.extractFeatures(imageBytes, "test.jpg");
        
        assertNotNull("成功时应返回特征向量", result);
        assertTrue("特征向量应该有效", result.isValid());
        assertEquals("Color特征维度应为256", 256, result.getColor().length);
        assertEquals("GLCM特征维度应为72", 72, result.getGlcm().length);
        assertEquals("LBP特征维度应为256", 256, result.getLbp().length);
        assertEquals("VGG特征维度应为512", 512, result.getVgg().length);
        assertEquals("VIT特征维度应为768", 768, result.getVit().length);
    }

    @Test
    public void testExtractAndSaveFeatures_Success() throws IOException {
        // 测试异步提取和保存特征的情况
        
        // 创建临时文件
        Path tempFile = Files.createTempFile("test", ".jpg");
        Files.write(tempFile, "fake image data".getBytes());
        
        try {
            // 模拟Docker服务响应
            Map<String, Object> dockerResponse = createMockDockerResponse();
            when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(dockerResponse, HttpStatus.OK));
            
            // 模拟数据保存成功
            when(dataService.saveFeatureVector(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(true);
            
            boolean result = dockerFeatureExtractionService.extractAndSaveFeatures(
                "test.jpg", tempFile.toString());
            
            assertTrue("异步提取请求应该成功提交", result);
            
            // 等待异步操作完成
            Thread.sleep(2000);
            
            // 验证数据保存被调用
            verify(dataService, timeout(3000)).saveFeatureVector(
                eq("test.jpg"), any(), any(), any(), any(), any());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testGetHealthStatus() {
        // 测试健康状态获取
        when(properties.isEnabled()).thenReturn(true);
        ResponseEntity<String> healthResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(healthResponse);
        
        String status = dockerFeatureExtractionService.getHealthStatus();
        
        assertEquals("健康状态应为正常", "Docker特征提取服务正常", status);
    }

    @Test
    public void testGetHealthStatus_WhenDisabled() {
        // 测试服务禁用时的健康状态
        when(properties.isEnabled()).thenReturn(false);
        
        String status = dockerFeatureExtractionService.getHealthStatus();
        
        assertEquals("禁用时的健康状态", "Docker特征提取服务已禁用", status);
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
