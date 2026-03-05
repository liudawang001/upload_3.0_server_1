package com.mls.upload.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 上传响应数据传输对象
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class UploadResponse {
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 上传状态（SUCCESS, FAILED, PROCESSING）
     */
    private String status;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 图片类型（1表示花型图，2表示配色图）
     */
    private String imageType;
    
    /**
     * 是否已提取特征
     */
    private Boolean featureExtracted;
    
    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
    
    /**
     * 错误信息（如果上传失败）
     */
    private String errorMessage;
    
    // 状态常量
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    
    // 默认构造函数
    public UploadResponse() {
        this.uploadTime = LocalDateTime.now();
    }
    
    // 带参构造函数
    public UploadResponse(String filename, String status) {
        this.filename = filename;
        this.status = status;
        this.uploadTime = LocalDateTime.now();
    }
    
    // 静态工厂方法
    public static UploadResponse success(String filename, String filePath, Long fileSize, String imageType) {
        UploadResponse response = new UploadResponse(filename, STATUS_SUCCESS);
        response.setFilePath(filePath);
        response.setFileSize(fileSize);
        response.setImageType(imageType);
        response.setFeatureExtracted(false);
        return response;
    }
    
    public static UploadResponse failed(String filename, String errorMessage) {
        UploadResponse response = new UploadResponse(filename, STATUS_FAILED);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public static UploadResponse processing(String filename) {
        return new UploadResponse(filename, STATUS_PROCESSING);
    }
    
    // Getter和Setter方法
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getImageType() {
        return imageType;
    }
    
    public void setImageType(String imageType) {
        this.imageType = imageType;
    }
    
    public Boolean getFeatureExtracted() {
        return featureExtracted;
    }
    
    public void setFeatureExtracted(Boolean featureExtracted) {
        this.featureExtracted = featureExtracted;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 判断上传是否成功
     * 
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(this.status);
    }
    
    /**
     * 判断是否正在处理中
     * 
     * @return true表示处理中，false表示已完成
     */
    public boolean isProcessing() {
        return STATUS_PROCESSING.equals(this.status);
    }
    
    /**
     * 判断是否失败
     * 
     * @return true表示失败，false表示成功或处理中
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(this.status);
    }
    
    @Override
    public String toString() {
        return "UploadResponse{" +
                "filename='" + filename + '\'' +
                ", status='" + status + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", imageType='" + imageType + '\'' +
                ", featureExtracted=" + featureExtracted +
                ", uploadTime=" + uploadTime +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
