package com.mls.upload.client.service;

import com.mls.upload.client.model.entity.ProductInfo;
import com.mls.upload.client.model.entity.ExcelRowData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.mls.upload.client.model.vo.LogEntry;

/**
 * Excel表格处理服务
 * 处理Excel文件的解析和数据提取
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    // 支持的Excel文件扩展名
    private static final String[] SUPPORTED_EXTENSIONS = {".xlsx", ".xls"};

    // 默认的商品编号列名
    private static final String[] PRODUCT_CODE_COLUMNS = {
        "商品编号", "产品编号", "编号", "货号", "商品号", "产品号",
        "PRODUCT_CODE", "PRODUCT_NO", "CODE", "NO", "SKU"
    };

    /**
     * 解析Excel文件，提取商品编号列表
     *
     * @param filePath Excel文件路径
     * @return 商品编号列表
     * @throws Exception 解析异常
     */
    public List<String> parseExcelFile(String filePath) throws Exception {
        logger.info("开始解析Excel文件: {}", filePath);

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Excel文件路径不能为空");
        }

        // 检查文件扩展名
        if (!isSupportedExcelFile(filePath)) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持.xlsx和.xls文件");
        }

        List<String> productCodes = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = createWorkbook(filePath, fis);

            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel文件中没有找到工作表");
            }

            // 查找商品编号列
            int productCodeColumnIndex = findProductCodeColumn(sheet);
            if (productCodeColumnIndex == -1) {
                throw new IllegalArgumentException("未找到商品编号列，请确保Excel文件包含以下列名之一: " +
                    String.join(", ", PRODUCT_CODE_COLUMNS));
            }

            logger.info("找到商品编号列，索引: {}", productCodeColumnIndex);

            // 读取数据行
            int dataRowCount = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Cell cell = row.getCell(productCodeColumnIndex);
                if (cell == null) {
                    continue;
                }

                String productCode = getCellValueAsString(cell);
                if (productCode != null && !productCode.trim().isEmpty()) {
                    productCodes.add(productCode.trim());
                    dataRowCount++;
                }
            }

            workbook.close();

            logger.info("Excel文件解析完成: 文件={}, 数据行数={}, 有效商品编号数={}",
                filePath, dataRowCount, productCodes.size());

            return productCodes;

        } catch (IOException e) {
            logger.error("Excel文件读取失败: {}", e.getMessage(), e);
            throw new Exception("Excel文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Excel文件，提取完整的商品信息列表
     *
     * @param filePath Excel文件路径
     * @return 商品信息列表
     * @throws Exception 解析异常
     */
    public List<ProductInfo> parseExcelFileToProductInfo(String filePath) throws Exception {
        logger.info("开始解析Excel文件为商品信息: {}", filePath);

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Excel文件路径不能为空");
        }

        List<ProductInfo> productInfos = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = createWorkbook(filePath, fis);
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException("Excel文件中没有找到工作表");
            }

            // 获取表头行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件中没有找到表头行");
            }

            // 解析表头，建立列索引映射
            ColumnMapping columnMapping = parseHeaderRow(headerRow);

            // 读取数据行
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                ProductInfo productInfo = parseDataRow(row, columnMapping);
                if (productInfo != null && productInfo.getProductCode() != null &&
                    !productInfo.getProductCode().trim().isEmpty()) {
                    productInfos.add(productInfo);
                }
            }

            workbook.close();

            logger.info("Excel文件解析为商品信息完成: 文件={}, 商品信息数={}", filePath, productInfos.size());

            return productInfos;

        } catch (IOException e) {
            logger.error("Excel文件读取失败: {}", e.getMessage(), e);
            throw new Exception("Excel文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否为支持的Excel文件
     *
     * @param filePath 文件路径
     * @return 是否支持
     */
    private boolean isSupportedExcelFile(String filePath) {
        if (filePath == null) {
            return false;
        }

        String lowerCasePath = filePath.toLowerCase();
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (lowerCasePath.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建Workbook对象
     *
     * @param filePath 文件路径
     * @param fis 文件输入流
     * @return Workbook对象
     * @throws IOException IO异常
     */
    private Workbook createWorkbook(String filePath, FileInputStream fis) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("不支持的Excel文件格式: " + filePath);
        }
    }

    /**
     * 查找商品编号列
     *
     * @param sheet 工作表
     * @return 列索引，-1表示未找到
     */
    private int findProductCodeColumn(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return -1;
        }

        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell == null) {
                continue;
            }

            String cellValue = getCellValueAsString(cell);
            if (cellValue != null) {
                cellValue = cellValue.trim();
                for (String columnName : PRODUCT_CODE_COLUMNS) {
                    if (columnName.equalsIgnoreCase(cellValue)) {
                        return cellIndex;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * 获取单元格值作为字符串
     *
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == Math.floor(numericValue)) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }

    /**
     * 列映射类
     */
    private static class ColumnMapping {
        private int productCodeIndex = -1;
        private int productNameIndex = -1;
        private int categoryIndex = -1;
        private int brandIndex = -1;
        private int specIndex = -1;
        private int remarkIndex = -1;

        // Getter和Setter方法
        public int getProductCodeIndex() { return productCodeIndex; }
        public void setProductCodeIndex(int productCodeIndex) { this.productCodeIndex = productCodeIndex; }

        public int getProductNameIndex() { return productNameIndex; }
        public void setProductNameIndex(int productNameIndex) { this.productNameIndex = productNameIndex; }

        public int getCategoryIndex() { return categoryIndex; }
        public void setCategoryIndex(int categoryIndex) { this.categoryIndex = categoryIndex; }

        public int getBrandIndex() { return brandIndex; }
        public void setBrandIndex(int brandIndex) { this.brandIndex = brandIndex; }

        public int getSpecIndex() { return specIndex; }
        public void setSpecIndex(int specIndex) { this.specIndex = specIndex; }

        public int getRemarkIndex() { return remarkIndex; }
        public void setRemarkIndex(int remarkIndex) { this.remarkIndex = remarkIndex; }
    }

    /**
     * 解析表头行，建立列映射
     *
     * @param headerRow 表头行
     * @return 列映射对象
     */
    private ColumnMapping parseHeaderRow(Row headerRow) {
        ColumnMapping mapping = new ColumnMapping();

        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell == null) {
                continue;
            }

            String cellValue = getCellValueAsString(cell);
            if (cellValue == null) {
                continue;
            }

            cellValue = cellValue.trim().toLowerCase();

            // 商品编号列
            if (isProductCodeColumn(cellValue)) {
                mapping.setProductCodeIndex(cellIndex);
            }
            // 商品名称列
            else if (isProductNameColumn(cellValue)) {
                mapping.setProductNameIndex(cellIndex);
            }
            // 分类列
            else if (isCategoryColumn(cellValue)) {
                mapping.setCategoryIndex(cellIndex);
            }
            // 品牌列
            else if (isBrandColumn(cellValue)) {
                mapping.setBrandIndex(cellIndex);
            }
            // 规格列
            else if (isSpecColumn(cellValue)) {
                mapping.setSpecIndex(cellIndex);
            }
            // 备注列
            else if (isRemarkColumn(cellValue)) {
                mapping.setRemarkIndex(cellIndex);
            }
        }

        return mapping;
    }

    /**
     * 解析数据行
     *
     * @param row 数据行
     * @param mapping 列映射
     * @return 商品信息对象
     */
    private ProductInfo parseDataRow(Row row, ColumnMapping mapping) {
        ProductInfo productInfo = new ProductInfo();

        // 商品编号（必填）
        if (mapping.getProductCodeIndex() >= 0) {
            Cell cell = row.getCell(mapping.getProductCodeIndex());
            String productCode = getCellValueAsString(cell);
            if (productCode == null || productCode.trim().isEmpty()) {
                return null; // 商品编号为空，跳过此行
            }
            productInfo.setProductCode(productCode.trim());
        } else {
            return null; // 没有商品编号列，跳过此行
        }

        // 商品名称
        if (mapping.getProductNameIndex() >= 0) {
            Cell cell = row.getCell(mapping.getProductNameIndex());
            String productName = getCellValueAsString(cell);
            productInfo.setProductName(productName != null ? productName.trim() : "");
        }

        // 分类
        if (mapping.getCategoryIndex() >= 0) {
            Cell cell = row.getCell(mapping.getCategoryIndex());
            String category = getCellValueAsString(cell);
            productInfo.setCategory(category != null ? category.trim() : "");
        }

        // 品牌
        if (mapping.getBrandIndex() >= 0) {
            Cell cell = row.getCell(mapping.getBrandIndex());
            String brand = getCellValueAsString(cell);
            productInfo.setBrand(brand != null ? brand.trim() : "");
        }

        // 规格
        if (mapping.getSpecIndex() >= 0) {
            Cell cell = row.getCell(mapping.getSpecIndex());
            String spec = getCellValueAsString(cell);
            productInfo.setSpec(spec != null ? spec.trim() : "");
        }

        // 备注
        if (mapping.getRemarkIndex() >= 0) {
            Cell cell = row.getCell(mapping.getRemarkIndex());
            String remark = getCellValueAsString(cell);
            productInfo.setRemark(remark != null ? remark.trim() : "");
        }

        return productInfo;
    }

    // 列名判断方法
    private boolean isProductCodeColumn(String columnName) {
        String[] names = {"商品编号", "产品编号", "编号", "货号", "商品号", "产品号",
                         "product_code", "product_no", "code", "no", "sku"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProductNameColumn(String columnName) {
        String[] names = {"商品名称", "产品名称", "名称", "商品名", "产品名",
                         "product_name", "name", "title"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCategoryColumn(String columnName) {
        String[] names = {"分类", "类别", "品类", "category", "type", "class"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBrandColumn(String columnName) {
        String[] names = {"品牌", "牌子", "brand", "manufacturer"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpecColumn(String columnName) {
        String[] names = {"规格", "型号", "尺寸", "spec", "specification", "model", "size"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRemarkColumn(String columnName) {
        String[] names = {"备注", "说明", "描述", "remark", "note", "description", "comment"};
        for (String name : names) {
            if (name.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证Excel文件格式
     *
     * @param filePath 文件路径
     * @return 验证结果
     */
    public boolean validateExcelFile(String filePath) {
        try {
            if (!isSupportedExcelFile(filePath)) {
                return false;
            }

            try (FileInputStream fis = new FileInputStream(filePath)) {
                Workbook workbook = createWorkbook(filePath, fis);
                Sheet sheet = workbook.getSheetAt(0);

                if (sheet == null || sheet.getLastRowNum() < 1) {
                    workbook.close();
                    return false;
                }

                // 检查是否有商品编号列
                int productCodeColumnIndex = findProductCodeColumn(sheet);
                workbook.close();

                return productCodeColumnIndex >= 0;
            }

        } catch (Exception e) {
            logger.error("Excel文件验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析Excel文件，提取完整的行数据列表
     * 用于花型图上传时同步插入pic_info表
     * 修改说明：从Map改为List结构，允许相同商品编号的多个条目独立存在
     *
     * @param filePath Excel文件路径
     * @return Excel行数据列表，支持重复商品编号
     * @throws Exception 解析异常
     */
    public List<ExcelRowData> parseExcelFileToRowDataMap(String filePath) throws Exception {
        logger.info("开始解析Excel文件为行数据列表（支持重复商品编号）: {}", filePath);

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Excel文件路径不能为空");
        }

        List<ExcelRowData> rowDataList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = createWorkbook(filePath, fis);
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException("Excel文件中没有找到工作表");
            }

            // 获取表头行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件中没有找到表头行");
            }

            // 解析表头，建立列索引映射
            Map<String, Integer> columnIndexMap = parseExcelHeader(headerRow);

            // 读取数据行
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                ExcelRowData rowData = parseExcelDataRow(row, columnIndexMap);
                if (rowData != null && rowData.getProductCode() != null &&
                    !rowData.getProductCode().trim().isEmpty()) {
                    // 修改：使用List.add()替代Map.put()，支持重复商品编号
                    rowDataList.add(rowData);
                }
            }

            workbook.close();

            logger.info("Excel文件解析为行数据列表完成: 文件={}, 行数据数={}", filePath, rowDataList.size());

            return rowDataList;

        } catch (IOException e) {
            logger.error("Excel文件读取失败: {}", e.getMessage(), e);
            throw new Exception("Excel文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Excel表头，建立列名到索引的映射
     *
     * @param headerRow 表头行
     * @return 列名到索引的映射
     */
    private Map<String, Integer> parseExcelHeader(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();

        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            if (cell != null) {
                String columnName = getCellValueAsString(cell);
                if (columnName != null && !columnName.trim().isEmpty()) {
                    columnIndexMap.put(columnName.trim(), cellIndex);
                }
            }
        }

        logger.debug("Excel表头解析完成，列映射: {}", columnIndexMap);
        return columnIndexMap;
    }

    /**
     * 解析Excel数据行，转换为ExcelRowData对象
     *
     * @param row 数据行
     * @param columnIndexMap 列索引映射
     * @return Excel行数据对象
     */
    private ExcelRowData parseExcelDataRow(Row row, Map<String, Integer> columnIndexMap) {
        ExcelRowData rowData = new ExcelRowData();

        // 商品编号（必填）
        String productCode = getCellValueByColumnName(row, columnIndexMap, "商品编号");
        if (productCode == null || productCode.trim().isEmpty()) {
            return null; // 商品编号为空，跳过此行
        }
        rowData.setProductCode(productCode.trim());

        // 其他字段（可选）
        rowData.setCategory(getCellValueByColumnName(row, columnIndexMap, "分类"));
        rowData.setOrderNumber(getCellValueByColumnName(row, columnIndexMap, "订单号"));
        rowData.setFabric(getCellValueByColumnName(row, columnIndexMap, "面料"));
        rowData.setCustomerName(getCellValueByColumnName(row, columnIndexMap, "客户名"));
        rowData.setMarket(getCellValueByColumnName(row, columnIndexMap, "市场"));
        rowData.setSetCount(getCellValueByColumnName(row, columnIndexMap, "套数"));
        rowData.setSoDate(getCellValueByColumnName(row, columnIndexMap, "SO日期"));
        rowData.setBulkDate(getCellValueByColumnName(row, columnIndexMap, "大货日期"));
        rowData.setFactoryCode(getCellValueByColumnName(row, columnIndexMap, "工厂编号"));
        rowData.setFactory(getCellValueByColumnName(row, columnIndexMap, "工厂"));
        rowData.setDesignCompany(getCellValueByColumnName(row, columnIndexMap, "描稿公司"));
        rowData.setDesigner(getCellValueByColumnName(row, columnIndexMap, "描稿人员"));
        rowData.setRemark(getCellValueByColumnName(row, columnIndexMap, "备注"));
        rowData.setMerchandiser(getCellValueByColumnName(row, columnIndexMap, "理单员"));
        rowData.setSortOrder(getCellValueByColumnName(row, columnIndexMap, "排序"));

        return rowData;
    }

    /**
     * 根据列名获取单元格值
     *
     * @param row 数据行
     * @param columnIndexMap 列索引映射
     * @param columnName 列名
     * @return 单元格值
     */
    private String getCellValueByColumnName(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer columnIndex = columnIndexMap.get(columnName);
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);
        String value = getCellValueAsString(cell);
        return value != null ? value.trim() : null;
    }

    /**
     * 导出失败记录到Excel文件
     *
     * @param failedEntries 失败的日志条目列表
     * @param headerColumns Excel标题行列名
     * @param filePath 导出文件路径
     * @throws Exception 导出异常
     */
    public void exportFailedRecordsToExcel(List<LogEntry> failedEntries,
                                         List<String> headerColumns,
                                         String filePath) throws Exception {
        logger.info("开始导出失败记录到Excel文件: {}, 失败记录数: {}", filePath, failedEntries.size());

        if (failedEntries == null || failedEntries.isEmpty()) {
            throw new IllegalArgumentException("没有失败记录可导出");
        }

        if (headerColumns == null || headerColumns.isEmpty()) {
            throw new IllegalArgumentException("Excel标题行不能为空");
        }

        // 创建新的Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("失败记录");

        try {
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderCellStyle(workbook);

            for (int i = 0; i < headerColumns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerColumns.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 写入失败记录数据
            int rowIndex = 1;
            for (LogEntry entry : failedEntries) {
                if (entry.getExcelRowData() != null) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    writeExcelRowData(dataRow, entry.getExcelRowData());
                }
            }

            // 优化列宽设置 - 确保所有内容完整显示
            adjustColumnWidths(sheet, headerColumns, failedEntries);

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }

            logger.info("失败记录导出完成: 文件={}, 记录数={}", filePath, rowIndex - 1);

        } finally {
            workbook.close();
        }
    }

    /**
     * 创建标题行样式
     *
     * @param workbook Excel工作簿
     * @return 标题行样式
     */
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // 设置背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 设置对齐方式
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 将ExcelRowData写入Excel行
     *
     * @param row Excel行
     * @param rowData Excel行数据
     */
    private void writeExcelRowData(Row row, ExcelRowData rowData) {
        // 按照标准的16列顺序写入数据
        setCellValue(row, 0, rowData.getProductCode());      // 商品编号
        setCellValue(row, 1, rowData.getCategory());         // 分类
        setCellValue(row, 2, rowData.getOrderNumber());      // 订单号
        setCellValue(row, 3, rowData.getFabric());           // 面料
        setCellValue(row, 4, rowData.getCustomerName());     // 客户名
        setCellValue(row, 5, rowData.getMarket());           // 市场
        setCellValue(row, 6, rowData.getSetCount());         // 套数
        setCellValue(row, 7, rowData.getSoDate());           // SO日期
        setCellValue(row, 8, rowData.getBulkDate());         // 大货日期
        setCellValue(row, 9, rowData.getFactoryCode());      // 工厂编号
        setCellValue(row, 10, rowData.getFactory());         // 工厂
        setCellValue(row, 11, rowData.getDesignCompany());   // 描稿公司
        setCellValue(row, 12, rowData.getDesigner());        // 描稿人员
        setCellValue(row, 13, rowData.getRemark());          // 备注
        setCellValue(row, 14, rowData.getMerchandiser());    // 理单员
        setCellValue(row, 15, rowData.getSortOrder());       // 排序
    }

    /**
     * 设置单元格值
     *
     * @param row Excel行
     * @param columnIndex 列索引
     * @param value 值
     */
    private void setCellValue(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value != null ? value : "");
    }

    /**
     * 优化Excel列宽设置，确保所有内容完整显示
     *
     * @param sheet Excel工作表
     * @param headerColumns 标题列
     * @param failedEntries 失败记录数据
     */
    private void adjustColumnWidths(Sheet sheet, List<String> headerColumns, List<LogEntry> failedEntries) {
        logger.debug("开始调整Excel列宽，列数: {}", headerColumns.size());

        // 定义每列的最小宽度（字符数）
        int[] minWidths = {
            12,  // 商品编号
            8,   // 分类
            15,  // 订单号
            10,  // 面料
            12,  // 客户名
            8,   // 市场
            6,   // 套数
            12,  // SO日期
            12,  // 大货日期
            12,  // 工厂编号
            15,  // 工厂
            12,  // 描稿公司
            10,  // 描稿人员
            20,  // 备注
            10,  // 理单员
            8    // 排序
        };

        for (int i = 0; i < headerColumns.size() && i < minWidths.length; i++) {
            // 首先使用自动调整
            sheet.autoSizeColumn(i);

            // 获取当前列宽（以1/256字符为单位）
            int currentWidth = sheet.getColumnWidth(i);

            // 计算标题宽度（标题长度 * 256 * 1.2倍边距）
            int headerWidth = (int) (headerColumns.get(i).length() * 256 * 1.2);

            // 计算最小宽度（最小字符数 * 256）
            int minWidth = minWidths[i] * 256;

            // 计算数据内容的最大宽度
            int maxDataWidth = calculateMaxDataWidth(failedEntries, i) * 256;

            // 选择最大的宽度，并添加适当边距
            int finalWidth = Math.max(Math.max(Math.max(currentWidth, headerWidth), minWidth), maxDataWidth);

            // 添加20%的边距，但不超过最大限制
            finalWidth = (int) (finalWidth * 1.2);

            // 设置最大宽度限制（避免列过宽）
            int maxWidth = 50 * 256; // 最大50个字符宽度
            finalWidth = Math.min(finalWidth, maxWidth);

            // 应用列宽
            sheet.setColumnWidth(i, finalWidth);

            logger.debug("列 {} ({}) 宽度设置: 当前={}, 标题={}, 最小={}, 数据={}, 最终={}",
                        i, headerColumns.get(i), currentWidth, headerWidth, minWidth, maxDataWidth, finalWidth);
        }

        logger.debug("Excel列宽调整完成");
    }

    /**
     * 计算指定列中数据内容的最大宽度
     *
     * @param failedEntries 失败记录数据
     * @param columnIndex 列索引
     * @return 最大字符长度
     */
    private int calculateMaxDataWidth(List<LogEntry> failedEntries, int columnIndex) {
        int maxLength = 0;

        for (LogEntry entry : failedEntries) {
            if (entry.getExcelRowData() != null) {
                String value = getColumnValue(entry.getExcelRowData(), columnIndex);
                if (value != null) {
                    maxLength = Math.max(maxLength, value.length());
                }
            }
        }

        return maxLength;
    }

    /**
     * 根据列索引获取对应的数据值
     *
     * @param rowData Excel行数据
     * @param columnIndex 列索引
     * @return 对应列的数据值
     */
    private String getColumnValue(ExcelRowData rowData, int columnIndex) {
        switch (columnIndex) {
            case 0: return rowData.getProductCode();      // 商品编号
            case 1: return rowData.getCategory();         // 分类
            case 2: return rowData.getOrderNumber();      // 订单号
            case 3: return rowData.getFabric();           // 面料
            case 4: return rowData.getCustomerName();     // 客户名
            case 5: return rowData.getMarket();           // 市场
            case 6: return rowData.getSetCount();         // 套数
            case 7: return rowData.getSoDate();           // SO日期
            case 8: return rowData.getBulkDate();         // 大货日期
            case 9: return rowData.getFactoryCode();      // 工厂编号
            case 10: return rowData.getFactory();        // 工厂
            case 11: return rowData.getDesignCompany();  // 描稿公司
            case 12: return rowData.getDesigner();       // 描稿人员
            case 13: return rowData.getRemark();         // 备注
            case 14: return rowData.getMerchandiser();   // 理单员
            case 15: return rowData.getSortOrder();      // 排序
            default: return "";
        }
    }
}
