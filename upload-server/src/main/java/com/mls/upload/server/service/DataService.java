package com.mls.upload.server.service;

import com.mls.upload.server.entity.DataVector;
import com.mls.upload.server.entity.ImageFeatureClip;
import com.mls.upload.server.entity.ImageUpload;
import com.mls.upload.server.entity.PicInfo;
import com.mls.upload.server.mapper.DataVectorMapper;
import com.mls.upload.server.mapper.ImageFeatureClipMapper;
import com.mls.upload.server.mapper.ImageUploadMapper;
import com.mls.upload.server.mapper.PicInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据服务
 * 处理数据管理和查询业务逻辑
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private DataVectorMapper dataVectorMapper;

    @Autowired
    private ImageFeatureClipMapper imageFeatureClipMapper;

    @Autowired
    private ImageUploadMapper imageUploadMapper;

    @Autowired
    private PicInfoMapper picInfoMapper;

    @Autowired
    private DockerFeatureExtractionService dockerFeatureExtractionService;

    /**
     * 获取数据统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getDataStatistics() {
        logger.info("获取数据统计信息");

        Map<String, Object> statistics = new HashMap<>();

        try {
            // 用户上传统计
            int totalUploads = imageUploadMapper.count();
            int flowerUploads = imageUploadMapper.countByImageType(ImageUpload.IMAGE_TYPE_FLOWER);
            int colorUploads = imageUploadMapper.countByImageType(ImageUpload.IMAGE_TYPE_COLOR);

            statistics.put("totalUploads", totalUploads);
            statistics.put("flowerUploads", flowerUploads);
            statistics.put("colorUploads", colorUploads);

            // 图片信息统计
            int totalPicInfo = picInfoMapper.count();
            statistics.put("totalPicInfo", totalPicInfo);

            // CLIP特征向量统计。保留totalVectors字段名，避免前端统计展示受影响。
            int totalVectors = imageFeatureClipMapper.count();
            List<String> extractedFiles = imageFeatureClipMapper.findExtractedFilenames();
            statistics.put("totalVectors", totalVectors);
            statistics.put("extractedFilesCount", extractedFiles.size());
            statistics.put("totalClipVectors", totalVectors);
            statistics.put("extractedClipFilesCount", extractedFiles.size());
            statistics.put("legacyDataVectors", dataVectorMapper.count());

            logger.info("数据统计信息获取成功: {}", statistics);

        } catch (Exception e) {
            logger.error("获取数据统计信息异常: {}", e.getMessage(), e);
        }

        return statistics;
    }

    /**
     * 根据商品编号查询图片信息
     *
     * @param productCode 商品编号
     * @return 图片信息列表
     */
    public List<PicInfo> getPicInfoByProductCode(String productCode) {
        if (!StringUtils.hasText(productCode)) {
            logger.warn("商品编号为空");
            return null;
        }

        try {
            List<PicInfo> picInfos = picInfoMapper.findByProductCode(productCode);
            logger.info("根据商品编号查询图片信息: productCode={}, count={}",
                       productCode, picInfos.size());
            return picInfos;
        } catch (Exception e) {
            logger.error("根据商品编号查询图片信息异常: productCode={}, error={}",
                        productCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取用户上传记录
     *
     * @param username 用户名
     * @return 上传记录列表
     */
    public List<ImageUpload> getUserUploadHistory(String username) {
        if (!StringUtils.hasText(username)) {
            logger.warn("用户名为空");
            return null;
        }

        try {
            List<ImageUpload> uploads = imageUploadMapper.findByUsername(username);
            logger.info("获取用户上传记录: username={}, count={}", username, uploads.size());
            return uploads;
        } catch (Exception e) {
            logger.error("获取用户上传记录异常: username={}, error={}",
                        username, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 请求特征提取
     *
     * @param filename 文件名
     * @param imagePath 图片路径
     * @return 特征提取是否成功
     */
    public boolean requestFeatureExtraction(String filename, String imagePath) {
        logger.info("请求特征提取: filename={}, imagePath={}", filename, imagePath);

        try {
            String normalizedFilename = normalizeFilename(filename);
            if (!StringUtils.hasText(normalizedFilename)) {
                logger.warn("文件名为空，跳过特征提取");
                return false;
            }

            // 检查是否已经提取过CLIP特征
            if (imageFeatureClipMapper.countByFilename(normalizedFilename) > 0) {
                if (!dockerFeatureExtractionService.isOverwriteEnabled()) {
                    logger.info("文件已存在CLIP特征向量，跳过提取: filename={}", normalizedFilename);
                    return true;
                } else {
                    logger.info("文件已存在CLIP特征向量，将重新提取并覆盖: filename={}", normalizedFilename);
                }
            }

            // 检查Docker服务是否可用
            if (!dockerFeatureExtractionService.isServiceAvailable()) {
                logger.warn("Docker特征提取服务不可用，跳过特征提取: filename={}", filename);
                return false;
            }

            // 使用新的Docker特征提取服务
            boolean success = dockerFeatureExtractionService.extractAndSaveFeatures(normalizedFilename, imagePath);

            if (success) {
                logger.info("特征提取请求提交成功: filename={}", filename);
                return true;
            } else {
                logger.warn("特征提取请求提交失败: filename={}", filename);
                return false;
            }

        } catch (Exception e) {
            logger.error("特征提取请求异常: filename={}, error={}", filename, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 保存特征向量数据
     *
     * @param filename 文件名
     * @param colorVector color特征向量
     * @param glcmVector glcm特征向量
     * @param lbpVector lbp特征向量
     * @param vggVector vgg特征向量
     * @param vitVector vit特征向量
     * @return 保存是否成功
     */
    public boolean saveFeatureVector(String filename, byte[] colorVector, byte[] glcmVector,
                                   byte[] lbpVector, byte[] vggVector, byte[] vitVector) {
        logger.info("保存特征向量: filename={}", filename);

        try {
            // 检查是否已存在
            DataVector existingVector = dataVectorMapper.findByFilename(filename);

            if (existingVector != null) {
                // 记录覆盖操作的详细信息
                LocalDateTime originalCreateTime = existingVector.getCreateTime();
                logger.info("覆盖现有特征向量: filename={}, 原记录ID={}, 原创建时间={}",
                           filename, existingVector.getId(), originalCreateTime);

                // 更新现有记录（包括特征向量数据和创建时间）
                existingVector.setColor(colorVector);
                existingVector.setGlcm(glcmVector);
                existingVector.setLbp(lbpVector);
                existingVector.setVgg(vggVector);
                existingVector.setVit(vitVector);
                // 更新创建时间为当前时间
                existingVector.setCreateTime(LocalDateTime.now());

                int result = dataVectorMapper.update(existingVector);

                if (result > 0) {
                    logger.info("特征向量覆盖成功: filename={}, 记录ID={}, 新创建时间={}",
                               filename, existingVector.getId(), existingVector.getCreateTime());
                    // 记录覆盖操作统计
                    com.mls.upload.server.util.PerformanceMonitor.recordSuccess(
                        com.mls.upload.server.util.PerformanceMonitor.TimerNames.FEATURE_VECTOR_OVERWRITE);
                    return true;
                } else {
                    logger.warn("特征向量覆盖失败: filename={}, 记录ID={}", filename, existingVector.getId());
                    com.mls.upload.server.util.PerformanceMonitor.recordError(
                        com.mls.upload.server.util.PerformanceMonitor.TimerNames.FEATURE_VECTOR_OVERWRITE);
                    return false;
                }
            } else {
                // 创建新记录
                DataVector newVector = new DataVector();
                newVector.setFilename(filename);
                newVector.setColor(colorVector);
                newVector.setGlcm(glcmVector);
                newVector.setLbp(lbpVector);
                newVector.setVgg(vggVector);
                newVector.setVit(vitVector);
                newVector.setCreateTime(LocalDateTime.now());

                int result = dataVectorMapper.insert(newVector);

                if (result > 0) {
                    logger.info("特征向量新增成功: filename={}, 记录ID={}", filename, newVector.getId());
                    // 记录新增操作统计
                    com.mls.upload.server.util.PerformanceMonitor.recordSuccess(
                        com.mls.upload.server.util.PerformanceMonitor.TimerNames.FEATURE_VECTOR_INSERT);
                    return true;
                } else {
                    logger.warn("特征向量新增失败: filename={}", filename);
                    com.mls.upload.server.util.PerformanceMonitor.recordError(
                        com.mls.upload.server.util.PerformanceMonitor.TimerNames.FEATURE_VECTOR_INSERT);
                    return false;
                }
            }

        } catch (Exception e) {
            logger.error("保存特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 保存CLIP特征向量数据到image_feature_clip表。
     *
     * @param filename 文件名
     * @param clipFeature CLIP特征BLOB，float32小端序
     * @param featureDim 特征维度
     * @param modelName 模型名称
     * @param rotationAugment 是否启用旋转增强
     * @param clipTime Docker提取耗时
     * @return 保存是否成功
     */
    public boolean saveClipFeatureVector(String filename, byte[] clipFeature, int featureDim,
                                         String modelName, boolean rotationAugment, Float clipTime) {
        String normalizedFilename = normalizeFilename(filename);
        logger.info("保存CLIP特征向量: filename={}, featureDim={}, blobSize={}",
                   normalizedFilename, featureDim, clipFeature == null ? 0 : clipFeature.length);

        try {
            if (!StringUtils.hasText(normalizedFilename)) {
                logger.warn("保存CLIP特征失败，文件名为空");
                return false;
            }

            int expectedBlobLength = featureDim * 4;
            if (clipFeature == null || clipFeature.length != expectedBlobLength) {
                logger.warn("保存CLIP特征失败，BLOB长度异常: filename={}, expected={}, actual={}",
                           normalizedFilename, expectedBlobLength, clipFeature == null ? 0 : clipFeature.length);
                return false;
            }

            ImageFeatureClip featureClip = new ImageFeatureClip();
            featureClip.setFilename(normalizedFilename);
            featureClip.setClipFeature(clipFeature);
            featureClip.setFeatureDim(featureDim);
            featureClip.setModelName(modelName);
            featureClip.setRotationAugment(rotationAugment ? 1 : 0);
            featureClip.setClipTime(clipTime);

            int result = imageFeatureClipMapper.insertOrUpdate(featureClip);
            if (result > 0) {
                logger.info("CLIP特征向量保存成功: filename={}, featureDim={}, modelName={}",
                           normalizedFilename, featureDim, modelName);
                return true;
            }

            logger.warn("CLIP特征向量保存失败: filename={}", normalizedFilename);
            return false;
        } catch (Exception e) {
            logger.error("保存CLIP特征向量异常: filename={}, error={}",
                        normalizedFilename, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取CLIP特征向量数据。
     */
    public ImageFeatureClip getClipFeatureVector(String filename) {
        if (!StringUtils.hasText(filename)) {
            logger.warn("文件名为空");
            return null;
        }

        try {
            ImageFeatureClip vector = imageFeatureClipMapper.findByFilename(normalizeFilename(filename));
            if (vector != null) {
                logger.info("获取CLIP特征向量成功: filename={}", filename);
            } else {
                logger.info("CLIP特征向量不存在: filename={}", filename);
            }
            return vector;
        } catch (Exception e) {
            logger.error("获取CLIP特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取特征向量数据
     *
     * @param filename 文件名
     * @return 特征向量数据
     */
    public DataVector getFeatureVector(String filename) {
        if (!StringUtils.hasText(filename)) {
            logger.warn("文件名为空");
            return null;
        }

        try {
            DataVector vector = dataVectorMapper.findByFilename(filename);
            if (vector != null) {
                logger.info("获取特征向量成功: filename={}", filename);
            } else {
                logger.info("特征向量不存在: filename={}", filename);
            }
            return vector;
        } catch (Exception e) {
            logger.error("获取特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除特征向量数据
     *
     * @param filename 文件名
     * @return 删除是否成功
     */
    public boolean deleteFeatureVector(String filename) {
        logger.info("删除特征向量: filename={}", filename);

        try {
            String normalizedFilename = normalizeFilename(filename);
            int clipResult = imageFeatureClipMapper.deleteByFilename(normalizedFilename);
            int legacyResult = dataVectorMapper.deleteByFilename(normalizedFilename);

            if (clipResult > 0 || legacyResult > 0) {
                logger.info("特征向量删除成功: filename={}, clipDeleted={}, legacyDeleted={}",
                           normalizedFilename, clipResult, legacyResult);
                return true;
            } else {
                logger.warn("特征向量删除失败，可能不存在: filename={}", normalizedFilename);
                return false;
            }

        } catch (Exception e) {
            logger.error("删除特征向量异常: filename={}, error={}", filename, e.getMessage(), e);
            return false;
        }
    }

    private String normalizeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        String normalized = filename.trim().replace("\\", "/");
        int lastSlashIndex = normalized.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < normalized.length() - 1) {
            normalized = normalized.substring(lastSlashIndex + 1);
        }

        return new File(normalized).getName();
    }
}
