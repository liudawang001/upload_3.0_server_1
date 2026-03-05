package com.mls.upload.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 图片信息实体类
 * 对应数据库pic_info表
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class PicInfo {

    /**
     * ID（主键，自增）
     */
    private Integer id;

    /**
     * 图片名称
     */
    private String filename;

    /**
     * 存储路径
     */
    private String filepath;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 商品编号
     */
    private String field01;

    /**
     * 分类
     */
    private String field02;

    /**
     * 订单号
     */
    private String field03;

    /**
     * 面料
     */
    private String field04;

    /**
     * 客户名
     */
    private String field05;

    /**
     * 市场
     */
    private String field06;

    /**
     * 套数
     */
    private String field07;

    /**
     * SO日期
     */
    private String field08;

    /**
     * 大货日期
     */
    private String field09;

    /**
     * 工厂编号
     */
    private String field10;

    /**
     * 工厂
     */
    private String field11;

    /**
     * 描稿公司
     */
    private String field12;

    /**
     * 描稿人员
     */
    private String field13;

    /**
     * 备注
     */
    private String field14;

    /**
     * 理单员
     */
    private String field15;

    /**
     * 排序号
     */
    private String field16;

    /**
     * 创建时间（字符串格式）
     */
    private String field17;

    /**
     * 描稿日期
     */
    private String field18;

    /**
     * 预留字段19
     */
    private String field19;

    /**
     * 预留字段20
     */
    private String field20;

    // 默认构造函数
    public PicInfo() {
    }

    // 带参构造函数
    public PicInfo(String filename, String filepath) {
        this.filename = filename;
        this.filepath = filepath;
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

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getField01() {
        return field01;
    }

    public void setField01(String field01) {
        this.field01 = field01;
    }

    public String getField02() {
        return field02;
    }

    public void setField02(String field02) {
        this.field02 = field02;
    }

    public String getField03() {
        return field03;
    }

    public void setField03(String field03) {
        this.field03 = field03;
    }

    public String getField04() {
        return field04;
    }

    public void setField04(String field04) {
        this.field04 = field04;
    }

    public String getField05() {
        return field05;
    }

    public void setField05(String field05) {
        this.field05 = field05;
    }

    public String getField06() {
        return field06;
    }

    public void setField06(String field06) {
        this.field06 = field06;
    }

    public String getField07() {
        return field07;
    }

    public void setField07(String field07) {
        this.field07 = field07;
    }

    public String getField08() {
        return field08;
    }

    public void setField08(String field08) {
        this.field08 = field08;
    }

    public String getField09() {
        return field09;
    }

    public void setField09(String field09) {
        this.field09 = field09;
    }

    public String getField10() {
        return field10;
    }

    public void setField10(String field10) {
        this.field10 = field10;
    }

    public String getField11() {
        return field11;
    }

    public void setField11(String field11) {
        this.field11 = field11;
    }

    public String getField12() {
        return field12;
    }

    public void setField12(String field12) {
        this.field12 = field12;
    }

    public String getField13() {
        return field13;
    }

    public void setField13(String field13) {
        this.field13 = field13;
    }

    public String getField14() {
        return field14;
    }

    public void setField14(String field14) {
        this.field14 = field14;
    }

    public String getField15() {
        return field15;
    }

    public void setField15(String field15) {
        this.field15 = field15;
    }

    public String getField16() {
        return field16;
    }

    public void setField16(String field16) {
        this.field16 = field16;
    }

    public String getField17() {
        return field17;
    }

    public void setField17(String field17) {
        this.field17 = field17;
    }

    public String getField18() {
        return field18;
    }

    public void setField18(String field18) {
        this.field18 = field18;
    }

    public String getField19() {
        return field19;
    }

    public void setField19(String field19) {
        this.field19 = field19;
    }

    public String getField20() {
        return field20;
    }

    public void setField20(String field20) {
        this.field20 = field20;
    }

    /**
     * 获取商品编号（field01的别名方法）
     *
     * @return 商品编号
     */
    public String getProductCode() {
        return field01;
    }

    /**
     * 设置商品编号（field01的别名方法）
     *
     * @param productCode 商品编号
     */
    public void setProductCode(String productCode) {
        this.field01 = productCode;
    }

    @Override
    public String toString() {
        return "PicInfo{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", filepath='" + filepath + '\'' +
                ", createTime=" + createTime +
                ", field01='" + field01 + '\'' +
                ", field02='" + field02 + '\'' +
                ", field03='" + field03 + '\'' +
                ", field04='" + field04 + '\'' +
                ", field05='" + field05 + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PicInfo picInfo = (PicInfo) o;

        return id != null ? id.equals(picInfo.id) : picInfo.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
