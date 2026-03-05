package com.mls.upload.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 特征向量实体类
 * 对应数据库data_vector表
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class DataVector {

    /**
     * 主键ID（自增）
     */
    private Integer id;

    /**
     * 图片名称
     */
    private String filename;

    /**
     * color特征向量
     */
    private byte[] color;

    /**
     * glcm特征向量
     */
    private byte[] glcm;

    /**
     * lbp特征向量
     */
    private byte[] lbp;

    /**
     * vgg特征向量
     */
    private byte[] vgg;

    /**
     * vit特征向量
     */
    private byte[] vit;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 默认构造函数
    public DataVector() {
    }

    // 带参构造函数
    public DataVector(String filename) {
        this.filename = filename;
        this.createTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getColor() {
        return color;
    }

    public void setColor(byte[] color) {
        this.color = color;
    }

    public byte[] getGlcm() {
        return glcm;
    }

    public void setGlcm(byte[] glcm) {
        this.glcm = glcm;
    }

    public byte[] getLbp() {
        return lbp;
    }

    public void setLbp(byte[] lbp) {
        this.lbp = lbp;
    }

    public byte[] getVgg() {
        return vgg;
    }

    public void setVgg(byte[] vgg) {
        this.vgg = vgg;
    }

    public byte[] getVit() {
        return vit;
    }

    public void setVit(byte[] vit) {
        this.vit = vit;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "DataVector{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", createTime=" + createTime +
                ", hasColorVector=" + (color != null && color.length > 0) +
                ", hasGlcmVector=" + (glcm != null && glcm.length > 0) +
                ", hasLbpVector=" + (lbp != null && lbp.length > 0) +
                ", hasVggVector=" + (vgg != null && vgg.length > 0) +
                ", hasVitVector=" + (vit != null && vit.length > 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataVector that = (DataVector) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
