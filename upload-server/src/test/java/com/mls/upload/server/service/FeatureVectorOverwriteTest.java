package com.mls.upload.server.service;

import com.mls.upload.server.entity.DataVector;
import com.mls.upload.server.mapper.DataVectorMapper;
import com.mls.upload.server.service.impl.DockerFeatureExtractionServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 特征向量覆盖功能测试
 * 验证覆盖已存在特征向量的功能是否正常工作
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class FeatureVectorOverwriteTest {

    private static final Logger logger = LoggerFactory.getLogger(FeatureVectorOverwriteTest.class);

    @Mock
    private DataVectorMapper dataVectorMapper;

    @Mock
    private DockerFeatureExtractionService dockerFeatureExtractionService;

    @InjectMocks
    private DataService dataService;

    private DataVector existingVector;
    private byte[] testColorVector;
    private byte[] testGlcmVector;
    private byte[] testLbpVector;
    private byte[] testVggVector;
    private byte[] testVitVector;

    @Before
    public void setUp() {
        // 创建测试数据
        existingVector = new DataVector();
        existingVector.setId(1);
        existingVector.setFilename("test.jpg");
        existingVector.setCreateTime(LocalDateTime.now().minusDays(1));
        
        // 创建测试特征向量数据
        testColorVector = new byte[]{1, 2, 3, 4};
        testGlcmVector = new byte[]{5, 6, 7, 8};
        testLbpVector = new byte[]{9, 10, 11, 12};
        testVggVector = new byte[]{13, 14, 15, 16};
        testVitVector = new byte[]{17, 18, 19, 20};
    }

    @Test
    public void testSaveFeatureVector_NewFile_ShouldInsert() {
        // 准备测试数据
        String filename = "new_test.jpg";

        // 模拟数据库操作
        when(dataVectorMapper.findByFilename(filename)).thenReturn(null);
        when(dataVectorMapper.insert(any(DataVector.class))).thenReturn(1);

        // 执行测试
        boolean result = dataService.saveFeatureVector(filename, testColorVector, testGlcmVector,
                                                     testLbpVector, testVggVector, testVitVector);

        // 验证结果
        assertTrue("新文件的特征向量保存应该成功", result);
        verify(dataVectorMapper, times(1)).findByFilename(filename);
        verify(dataVectorMapper, times(1)).insert(any(DataVector.class));
        verify(dataVectorMapper, never()).update(any(DataVector.class));

        logger.info("✅ 新文件特征向量保存测试通过");
    }

    @Test
    public void testSaveFeatureVector_ExistingFile_ShouldUpdate() {
        // 准备测试数据
        String filename = "existing_test.jpg";

        // 模拟数据库操作
        when(dataVectorMapper.findByFilename(filename)).thenReturn(existingVector);
        when(dataVectorMapper.update(any(DataVector.class))).thenReturn(1);

        // 执行测试
        boolean result = dataService.saveFeatureVector(filename, testColorVector, testGlcmVector,
                                                     testLbpVector, testVggVector, testVitVector);

        // 验证结果
        assertTrue("已存在文件的特征向量覆盖应该成功", result);
        verify(dataVectorMapper, times(1)).findByFilename(filename);
        verify(dataVectorMapper, times(1)).update(any(DataVector.class));
        verify(dataVectorMapper, never()).insert(any(DataVector.class));

        // 验证特征向量数据被正确设置
        assertEquals(testColorVector, existingVector.getColor());
        assertEquals(testGlcmVector, existingVector.getGlcm());
        assertEquals(testLbpVector, existingVector.getLbp());
        assertEquals(testVggVector, existingVector.getVgg());
        assertEquals(testVitVector, existingVector.getVit());

        logger.info("✅ 已存在文件特征向量覆盖测试通过");
    }

    @Test
    public void testRequestFeatureExtraction_OverwriteDisabled_ShouldSkip() {
        // 准备测试数据
        String filename = "existing_test.jpg";
        String imagePath = "/path/to/existing_test.jpg";

        // 模拟配置和数据库操作
        when(dataVectorMapper.countByFilename(filename)).thenReturn(1);
        when(dockerFeatureExtractionService.isOverwriteEnabled()).thenReturn(false);

        // 执行测试
        boolean result = dataService.requestFeatureExtraction(filename, imagePath);

        // 验证结果
        assertTrue("禁用覆盖时应该跳过已存在文件并返回成功", result);
        verify(dataVectorMapper, times(1)).countByFilename(filename);
        verify(dockerFeatureExtractionService, times(1)).isOverwriteEnabled();
        verify(dockerFeatureExtractionService, never()).isServiceAvailable();
        verify(dockerFeatureExtractionService, never()).extractAndSaveFeatures(anyString(), anyString());

        logger.info("✅ 禁用覆盖时跳过已存在文件测试通过");
    }

    @Test
    public void testRequestFeatureExtraction_OverwriteEnabled_ShouldProceed() {
        // 准备测试数据
        String filename = "existing_test.jpg";
        String imagePath = "/path/to/existing_test.jpg";

        // 模拟配置和服务操作
        when(dataVectorMapper.countByFilename(filename)).thenReturn(1);
        when(dockerFeatureExtractionService.isOverwriteEnabled()).thenReturn(true);
        when(dockerFeatureExtractionService.isServiceAvailable()).thenReturn(true);
        when(dockerFeatureExtractionService.extractAndSaveFeatures(filename, imagePath)).thenReturn(true);

        // 执行测试
        boolean result = dataService.requestFeatureExtraction(filename, imagePath);

        // 验证结果
        assertTrue("启用覆盖时应该继续处理已存在文件", result);
        verify(dataVectorMapper, times(1)).countByFilename(filename);
        verify(dockerFeatureExtractionService, times(1)).isOverwriteEnabled();
        verify(dockerFeatureExtractionService, times(1)).isServiceAvailable();
        verify(dockerFeatureExtractionService, times(1)).extractAndSaveFeatures(filename, imagePath);

        logger.info("✅ 启用覆盖时继续处理已存在文件测试通过");
    }

    @Test
    public void testSaveFeatureVector_UpdateFailure_ShouldReturnFalse() {
        // 准备测试数据
        String filename = "existing_test.jpg";

        // 模拟数据库操作失败
        when(dataVectorMapper.findByFilename(filename)).thenReturn(existingVector);
        when(dataVectorMapper.update(any(DataVector.class))).thenReturn(0);

        // 执行测试
        boolean result = dataService.saveFeatureVector(filename, testColorVector, testGlcmVector,
                                                     testLbpVector, testVggVector, testVitVector);

        // 验证结果
        assertFalse("数据库更新失败时应该返回false", result);
        verify(dataVectorMapper, times(1)).findByFilename(filename);
        verify(dataVectorMapper, times(1)).update(any(DataVector.class));

        logger.info("✅ 数据库更新失败处理测试通过");
    }

    @Test
    public void testSaveFeatureVector_InsertFailure_ShouldReturnFalse() {
        // 准备测试数据
        String filename = "new_test.jpg";

        // 模拟数据库操作失败
        when(dataVectorMapper.findByFilename(filename)).thenReturn(null);
        when(dataVectorMapper.insert(any(DataVector.class))).thenReturn(0);

        // 执行测试
        boolean result = dataService.saveFeatureVector(filename, testColorVector, testGlcmVector,
                                                     testLbpVector, testVggVector, testVitVector);

        // 验证结果
        assertFalse("数据库插入失败时应该返回false", result);
        verify(dataVectorMapper, times(1)).findByFilename(filename);
        verify(dataVectorMapper, times(1)).insert(any(DataVector.class));

        logger.info("✅ 数据库插入失败处理测试通过");
    }

    @Test
    public void testSaveFeatureVector_DatabaseException_ShouldReturnFalse() {
        // 准备测试数据
        String filename = "test.jpg";

        // 模拟数据库异常
        when(dataVectorMapper.findByFilename(filename)).thenThrow(new RuntimeException("数据库连接异常"));

        // 执行测试
        boolean result = dataService.saveFeatureVector(filename, testColorVector, testGlcmVector,
                                                     testLbpVector, testVggVector, testVitVector);

        // 验证结果
        assertFalse("数据库异常时应该返回false", result);
        verify(dataVectorMapper, times(1)).findByFilename(filename);

        logger.info("✅ 数据库异常处理测试通过");
    }
}
