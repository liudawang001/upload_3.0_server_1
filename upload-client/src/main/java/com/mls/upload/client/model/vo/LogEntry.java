package com.mls.upload.client.model.vo;

import com.mls.upload.client.model.entity.ExcelRowData;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;

/**
 * 日志条目视图对象
 * 用于界面日志显示
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class LogEntry {

    private Integer index;           // 序号
    private String productCode;      // 商品编号
    private String filename;         // 图片名称
    private String filePath;         // 图片完整路径
    private String matchStatus;      // 比对状态
    private String uploadStatus;     // 上传状态
    private String progress;         // 进度
    private String message;          // 备注信息
    private String time;             // 时间
    private ExcelRowData excelRowData; // Excel行数据（用于花型图上传时同步插入pic_info）
    private BooleanProperty selected; // 选中状态，默认为true

    // 默认构造函数
    public LogEntry() {
        this.selected = new SimpleBooleanProperty(true); // 默认选中
    }

    // 带参数构造函数
    public LogEntry(Integer index, String productCode, String filename,
                   String matchStatus, String uploadStatus, String progress,
                   String message, String time) {
        this.index = index;
        this.productCode = productCode;
        this.filename = filename;
        this.matchStatus = matchStatus;
        this.uploadStatus = uploadStatus;
        this.progress = progress;
        this.message = message;
        this.time = time;
        this.selected = new SimpleBooleanProperty(true); // 默认选中
    }

    // 带文件路径的构造函数
    public LogEntry(Integer index, String productCode, String filename, String filePath,
                   String matchStatus, String uploadStatus, String progress,
                   String message, String time) {
        this.index = index;
        this.productCode = productCode;
        this.filename = filename;
        this.filePath = filePath;
        this.matchStatus = matchStatus;
        this.uploadStatus = uploadStatus;
        this.progress = progress;
        this.message = message;
        this.time = time;
        this.selected = new SimpleBooleanProperty(true); // 默认选中
    }

    // Getter和Setter方法
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public ExcelRowData getExcelRowData() {
        return excelRowData;
    }

    public void setExcelRowData(ExcelRowData excelRowData) {
        this.excelRowData = excelRowData;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "index=" + index +
                ", productCode='" + productCode + '\'' +
                ", filename='" + filename + '\'' +
                ", filePath='" + filePath + '\'' +
                ", matchStatus='" + matchStatus + '\'' +
                ", uploadStatus='" + uploadStatus + '\'' +
                ", progress='" + progress + '\'' +
                ", message='" + message + '\'' +
                ", time='" + time + '\'' +
                ", excelRowData=" + excelRowData +
                '}';
    }
}
