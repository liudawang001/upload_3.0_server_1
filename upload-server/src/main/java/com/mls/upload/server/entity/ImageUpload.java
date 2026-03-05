package com.mls.upload.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 图片上传记录实体类
 * 对应数据库image_upload表
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ImageUpload {

    /**
     * ID（主键，自增）
     */
    private Integer id;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 图片类型（1表示花型图，2表示配色图）
     */
    private String imageType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 用户名（记录哪一个用户上传的文件）
     */
    private String username;

    // 图片类型常量
    public static final String IMAGE_TYPE_FLOWER = "1";  // 花型图
    public static final String IMAGE_TYPE_COLOR = "2";   // 配色图

    // 默认构造函数
    public ImageUpload() {
    }

    // 带参构造函数
    public ImageUpload(String filename, String imageType, String username) {
        this.filename = filename;
        this.imageType = imageType;
        this.username = username;
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

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 判断是否为花型图
     *
     * @return true表示花型图，false表示配色图
     */
    public boolean isFlowerImage() {
        return IMAGE_TYPE_FLOWER.equals(this.imageType);
    }

    /**
     * 判断是否为配色图
     *
     * @return true表示配色图，false表示花型图
     */
    public boolean isColorImage() {
        return IMAGE_TYPE_COLOR.equals(this.imageType);
    }

    @Override
    public String toString() {
        return "ImageUpload{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", imageType='" + imageType + '\'' +
                ", createTime=" + createTime +
                ", username='" + username + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageUpload that = (ImageUpload) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
