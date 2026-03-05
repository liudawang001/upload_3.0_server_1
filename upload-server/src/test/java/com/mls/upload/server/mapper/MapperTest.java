package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.PicUser;
import com.mls.upload.server.entity.ImageUpload;
import com.mls.upload.server.entity.PicInfo;
import com.mls.upload.server.entity.DataVector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper接口测试类
 * 测试数据访问层的基本功能
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional // 测试后回滚，不影响数据库
public class MapperTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MapperTest.class);
    
    @Autowired
    private PicUserMapper picUserMapper;
    
    @Autowired
    private ImageUploadMapper imageUploadMapper;
    
    @Autowired
    private PicInfoMapper picInfoMapper;
    
    @Autowired
    private DataVectorMapper dataVectorMapper;
    
    /**
     * 测试PicUserMapper基本功能
     */
    @Test
    public void testPicUserMapper() {
        logger.info("开始测试PicUserMapper...");
        
        // 测试查询所有用户
        List<PicUser> users = picUserMapper.findAll();
        logger.info("查询到用户数量: {}", users.size());
        
        if (!users.isEmpty()) {
            PicUser firstUser = users.get(0);
            logger.info("第一个用户信息: {}", firstUser);
            
            // 测试根据用户名查询
            PicUser userByName = picUserMapper.findByUsername(firstUser.getUsername());
            logger.info("根据用户名查询结果: {}", userByName);
            
            // 测试根据ID查询
            PicUser userById = picUserMapper.findById(firstUser.getId());
            logger.info("根据ID查询结果: {}", userById);
        }
        
        // 测试统计功能
        int userCount = picUserMapper.countByUsername("test_user");
        logger.info("test_user用户数量: {}", userCount);
        
        logger.info("PicUserMapper测试完成");
    }
    
    /**
     * 测试ImageUploadMapper基本功能
     */
    @Test
    public void testImageUploadMapper() {
        logger.info("开始测试ImageUploadMapper...");
        
        // 测试查询所有上传记录
        List<ImageUpload> uploads = imageUploadMapper.findAll();
        logger.info("查询到上传记录数量: {}", uploads.size());
        
        // 测试统计功能
        int totalCount = imageUploadMapper.count();
        logger.info("上传记录总数: {}", totalCount);
        
        int flowerCount = imageUploadMapper.countByImageType("1");
        logger.info("花型图上传记录数: {}", flowerCount);
        
        int colorCount = imageUploadMapper.countByImageType("2");
        logger.info("配色图上传记录数: {}", colorCount);
        
        logger.info("ImageUploadMapper测试完成");
    }
    
    /**
     * 测试PicInfoMapper基本功能
     */
    @Test
    public void testPicInfoMapper() {
        logger.info("开始测试PicInfoMapper...");
        
        // 测试统计功能
        int totalCount = picInfoMapper.count();
        logger.info("图片信息记录总数: {}", totalCount);
        
        // 测试查询前10条记录
        List<PicInfo> picInfos = picInfoMapper.findAll();
        if (picInfos.size() > 10) {
            picInfos = picInfos.subList(0, 10);
        }
        logger.info("查询到图片信息数量: {}", picInfos.size());
        
        if (!picInfos.isEmpty()) {
            PicInfo firstPic = picInfos.get(0);
            logger.info("第一条图片信息: filename={}, field01={}", 
                       firstPic.getFilename(), firstPic.getField01());
            
            // 测试根据文件名查询
            PicInfo picByName = picInfoMapper.findByFilename(firstPic.getFilename());
            if (picByName != null) {
                logger.info("根据文件名查询成功: {}", picByName.getFilename());
            }
        }
        
        logger.info("PicInfoMapper测试完成");
    }
    
    /**
     * 测试DataVectorMapper基本功能
     */
    @Test
    public void testDataVectorMapper() {
        logger.info("开始测试DataVectorMapper...");
        
        // 测试统计功能
        int totalCount = dataVectorMapper.count();
        logger.info("特征向量记录总数: {}", totalCount);
        
        // 测试查询已提取特征的文件列表
        List<String> extractedFiles = dataVectorMapper.findExtractedFilenames();
        logger.info("已提取特征的文件数量: {}", extractedFiles.size());
        
        if (!extractedFiles.isEmpty()) {
            String firstFile = extractedFiles.get(0);
            logger.info("第一个已提取特征的文件: {}", firstFile);
            
            // 测试根据文件名查询
            DataVector vector = dataVectorMapper.findByFilename(firstFile);
            if (vector != null) {
                logger.info("特征向量信息: filename={}, hasColor={}, hasVgg={}", 
                           vector.getFilename(), 
                           vector.getColor() != null && vector.getColor().length > 0,
                           vector.getVgg() != null && vector.getVgg().length > 0);
            }
        }
        
        logger.info("DataVectorMapper测试完成");
    }
    
    /**
     * 测试所有Mapper的基本连接
     */
    @Test
    public void testAllMappersConnection() {
        logger.info("开始测试所有Mapper连接...");
        
        try {
            // 测试每个Mapper的基本查询
            int userCount = picUserMapper.countByUsername("test");
            int uploadCount = imageUploadMapper.count();
            int picCount = picInfoMapper.count();
            int vectorCount = dataVectorMapper.count();
            
            logger.info("数据库连接测试结果:");
            logger.info("- 用户表连接正常，测试查询返回: {}", userCount);
            logger.info("- 上传记录表连接正常，总记录数: {}", uploadCount);
            logger.info("- 图片信息表连接正常，总记录数: {}", picCount);
            logger.info("- 特征向量表连接正常，总记录数: {}", vectorCount);
            
            logger.info("所有Mapper连接测试成功！");
            
        } catch (Exception e) {
            logger.error("Mapper连接测试失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
