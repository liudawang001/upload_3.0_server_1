package com.mls.upload.server.mapper;

import com.mls.upload.server.entity.ImageFeatureClip;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CLIP特征数据访问接口，处理image_feature_clip表。
 */
@Mapper
public interface ImageFeatureClipMapper {

    @Insert("INSERT INTO image_feature_clip " +
            "(filename, clip_feature, feature_dim, model_name, rotation_augment, clip_time, create_time, update_time) " +
            "VALUES (#{filename}, #{clipFeature}, #{featureDim}, #{modelName}, #{rotationAugment}, #{clipTime}, " +
            "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON DUPLICATE KEY UPDATE " +
            "clip_feature = VALUES(clip_feature), " +
            "feature_dim = VALUES(feature_dim), " +
            "model_name = VALUES(model_name), " +
            "rotation_augment = VALUES(rotation_augment), " +
            "clip_time = VALUES(clip_time), " +
            "update_time = CURRENT_TIMESTAMP")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrUpdate(ImageFeatureClip imageFeatureClip);

    @Select("SELECT id, filename, clip_feature, feature_dim, model_name, rotation_augment, clip_time, create_time, update_time " +
            "FROM image_feature_clip WHERE filename = #{filename}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "filename", column = "filename"),
        @Result(property = "clipFeature", column = "clip_feature"),
        @Result(property = "featureDim", column = "feature_dim"),
        @Result(property = "modelName", column = "model_name"),
        @Result(property = "rotationAugment", column = "rotation_augment"),
        @Result(property = "clipTime", column = "clip_time"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    ImageFeatureClip findByFilename(@Param("filename") String filename);

    @Select("SELECT COUNT(*) FROM image_feature_clip")
    int count();

    @Select("SELECT COUNT(*) FROM image_feature_clip WHERE filename = #{filename}")
    int countByFilename(@Param("filename") String filename);

    @Delete("DELETE FROM image_feature_clip WHERE filename = #{filename}")
    int deleteByFilename(@Param("filename") String filename);

    @Select("SELECT DISTINCT filename FROM image_feature_clip ORDER BY filename")
    List<String> findExtractedFilenames();
}
