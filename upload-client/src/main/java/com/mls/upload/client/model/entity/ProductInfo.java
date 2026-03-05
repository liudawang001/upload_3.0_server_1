package com.mls.upload.client.model.entity;

/**
 * 商品信息实体类
 * 对应Excel表格中的商品数据
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ProductInfo {

    private String productCode;    // 商品编号
    private String productName;    // 商品名称
    private String category;       // 分类
    private String brand;          // 品牌
    private String spec;           // 规格
    private String remark;         // 备注

    // 默认构造函数
    public ProductInfo() {
    }

    // 带参数构造函数
    public ProductInfo(String productCode, String productName) {
        this.productCode = productCode;
        this.productName = productName;
    }

    // 完整构造函数
    public ProductInfo(String productCode, String productName, String category,
                      String brand, String spec, String remark) {
        this.productCode = productCode;
        this.productName = productName;
        this.category = category;
        this.brand = brand;
        this.spec = spec;
        this.remark = remark;
    }

    // Getter和Setter方法
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "ProductInfo{" +
                "productCode='" + productCode + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                ", spec='" + spec + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductInfo that = (ProductInfo) o;

        return productCode != null ? productCode.equals(that.productCode) : that.productCode == null;
    }

    @Override
    public int hashCode() {
        return productCode != null ? productCode.hashCode() : 0;
    }
}
