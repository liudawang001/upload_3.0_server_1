package com.mls.upload.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * CLIP特征实体，对应image_feature_clip表。
 */
public class ImageFeatureClip {

    private Long id;

    private String filename;

    private byte[] clipFeature;

    private Integer featureDim;

    private String modelName;

    /**
     * 数据库使用tinyint/int保存布尔值：1表示启用旋转增强，0表示未启用。
     */
    private Integer rotationAugment;

    private Float clipTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getClipFeature() {
        return clipFeature;
    }

    public void setClipFeature(byte[] clipFeature) {
        this.clipFeature = clipFeature;
    }

    public Integer getFeatureDim() {
        return featureDim;
    }

    public void setFeatureDim(Integer featureDim) {
        this.featureDim = featureDim;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getRotationAugment() {
        return rotationAugment;
    }

    public void setRotationAugment(Integer rotationAugment) {
        this.rotationAugment = rotationAugment;
    }

    public Float getClipTime() {
        return clipTime;
    }

    public void setClipTime(Float clipTime) {
        this.clipTime = clipTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "ImageFeatureClip{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", featureDim=" + featureDim +
                ", modelName='" + modelName + '\'' +
                ", rotationAugment=" + rotationAugment +
                ", clipTime=" + clipTime +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", hasClipFeature=" + (clipFeature != null && clipFeature.length > 0) +
                '}';
    }
}
