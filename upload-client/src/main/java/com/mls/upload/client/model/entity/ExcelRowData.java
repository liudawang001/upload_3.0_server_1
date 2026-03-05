package com.mls.upload.client.model.entity;

/**
 * Excel行数据实体类
 * 对应Excel表格中的完整行数据，映射到pic_info表的字段
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ExcelRowData {

    /**
     * 商品编号 - 对应pic_info.field01
     */
    private String productCode;

    /**
     * 分类 - 对应pic_info.field02
     */
    private String category;

    /**
     * 订单号 - 对应pic_info.field03
     */
    private String orderNumber;

    /**
     * 面料 - 对应pic_info.field04
     */
    private String fabric;

    /**
     * 客户名 - 对应pic_info.field05
     */
    private String customerName;

    /**
     * 市场 - 对应pic_info.field06
     */
    private String market;

    /**
     * 套数 - 对应pic_info.field07
     */
    private String setCount;

    /**
     * SO日期 - 对应pic_info.field08
     */
    private String soDate;

    /**
     * 大货日期 - 对应pic_info.field09
     */
    private String bulkDate;

    /**
     * 工厂编号 - 对应pic_info.field10
     */
    private String factoryCode;

    /**
     * 工厂 - 对应pic_info.field11
     */
    private String factory;

    /**
     * 描稿公司 - 对应pic_info.field12
     */
    private String designCompany;

    /**
     * 描稿人员 - 对应pic_info.field13
     */
    private String designer;

    /**
     * 备注 - 对应pic_info.field14
     */
    private String remark;

    /**
     * 理单员 - 对应pic_info.field15
     */
    private String merchandiser;

    /**
     * 排序 - 对应pic_info.field16
     */
    private String sortOrder;

    // 默认构造函数
    public ExcelRowData() {
    }

    // 带商品编号的构造函数
    public ExcelRowData(String productCode) {
        this.productCode = productCode;
    }

    // 完整构造函数
    public ExcelRowData(String productCode, String category, String orderNumber, String fabric,
                       String customerName, String market, String setCount, String soDate,
                       String bulkDate, String factoryCode, String factory, String designCompany,
                       String designer, String remark, String merchandiser, String sortOrder) {
        this.productCode = productCode;
        this.category = category;
        this.orderNumber = orderNumber;
        this.fabric = fabric;
        this.customerName = customerName;
        this.market = market;
        this.setCount = setCount;
        this.soDate = soDate;
        this.bulkDate = bulkDate;
        this.factoryCode = factoryCode;
        this.factory = factory;
        this.designCompany = designCompany;
        this.designer = designer;
        this.remark = remark;
        this.merchandiser = merchandiser;
        this.sortOrder = sortOrder;
    }

    // Getter和Setter方法
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getFabric() {
        return fabric;
    }

    public void setFabric(String fabric) {
        this.fabric = fabric;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSetCount() {
        return setCount;
    }

    public void setSetCount(String setCount) {
        this.setCount = setCount;
    }

    public String getSoDate() {
        return soDate;
    }

    public void setSoDate(String soDate) {
        this.soDate = soDate;
    }

    public String getBulkDate() {
        return bulkDate;
    }

    public void setBulkDate(String bulkDate) {
        this.bulkDate = bulkDate;
    }

    public String getFactoryCode() {
        return factoryCode;
    }

    public void setFactoryCode(String factoryCode) {
        this.factoryCode = factoryCode;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public String getDesignCompany() {
        return designCompany;
    }

    public void setDesignCompany(String designCompany) {
        this.designCompany = designCompany;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getMerchandiser() {
        return merchandiser;
    }

    public void setMerchandiser(String merchandiser) {
        this.merchandiser = merchandiser;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "ExcelRowData{" +
                "productCode='" + productCode + '\'' +
                ", category='" + category + '\'' +
                ", orderNumber='" + orderNumber + '\'' +
                ", fabric='" + fabric + '\'' +
                ", customerName='" + customerName + '\'' +
                ", market='" + market + '\'' +
                ", setCount='" + setCount + '\'' +
                ", soDate='" + soDate + '\'' +
                ", bulkDate='" + bulkDate + '\'' +
                ", factoryCode='" + factoryCode + '\'' +
                ", factory='" + factory + '\'' +
                ", designCompany='" + designCompany + '\'' +
                ", designer='" + designer + '\'' +
                ", remark='" + remark + '\'' +
                ", merchandiser='" + merchandiser + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}
