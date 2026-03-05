package com.mls.upload.server.dto;

import com.mls.upload.server.entity.PicInfo;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 上传请求数据传输对象
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class UploadRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String filename;

    /**
     * 图片类型（1表示花型图，2表示配色图）
     */
    @NotBlank(message = "图片类型不能为空")
    @Pattern(regexp = "^[12]$", message = "图片类型只能是1（花型图）或2（配色图）")
    private String imageType;

    /**
     * Base64编码的图片数据
     */
    @NotBlank(message = "图片数据不能为空")
    private String base64Data;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 图片信息（Excel表格数据）
     */
    private PicInfo picInfo;

    /**
     * Excel数据（用于花型图上传时同步插入pic_info表）
     */
    private java.util.Map<String, Object> excelData;

    /**
     * 是否需要特征提取（仅花型图需要）
     */
    private Boolean needFeatureExtraction;

    // 默认构造函数
    public UploadRequest() {
    }

    // 带参构造函数
    public UploadRequest(String filename, String imageType, String base64Data, String username) {
        this.filename = filename;
        this.imageType = imageType;
        this.base64Data = base64Data;
        this.username = username;
        this.needFeatureExtraction = "1".equals(imageType); // 花型图需要特征提取
    }

    // Getter和Setter方法
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
        // 自动设置是否需要特征提取
        this.needFeatureExtraction = "1".equals(imageType);
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PicInfo getPicInfo() {
        return picInfo;
    }

    public void setPicInfo(PicInfo picInfo) {
        this.picInfo = picInfo;
    }

    public Boolean getNeedFeatureExtraction() {
        return needFeatureExtraction;
    }

    public void setNeedFeatureExtraction(Boolean needFeatureExtraction) {
        this.needFeatureExtraction = needFeatureExtraction;
    }

    /**
     * 判断是否为花型图
     *
     * @return true表示花型图，false表示配色图
     */
    public boolean isFlowerImage() {
        return "1".equals(this.imageType);
    }

    /**
     * 判断是否为配色图
     *
     * @return true表示配色图，false表示花型图
     */
    public boolean isColorImage() {
        return "2".equals(this.imageType);
    }

    public java.util.Map<String, Object> getExcelData() {
        return excelData;
    }

    public void setExcelData(java.util.Map<String, Object> excelData) {
        this.excelData = excelData;
    }

    @Override
    public String toString() {
        return "UploadRequest{" +
                "filename='" + filename + '\'' +
                ", imageType='" + imageType + '\'' +
                ", username='" + username + '\'' +
                ", needFeatureExtraction=" + needFeatureExtraction +
                ", hasBase64Data=" + (base64Data != null && !base64Data.isEmpty()) +
                ", hasPicInfo=" + (picInfo != null) +
                ", hasExcelData=" + (excelData != null && !excelData.isEmpty()) +
                '}';
    }
}
