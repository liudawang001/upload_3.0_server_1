package com.mls.upload.client.service;

import com.mls.upload.client.model.entity.ExcelRowData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * 重复商品编号支持功能测试
 * 验证Excel解析能够正确处理重复的商品编号
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class DuplicateProductCodeTest {

    private ExcelService excelService;
    private File tempDir;

    @Before
    public void setUp() throws IOException {
        excelService = new ExcelService();
        // 创建临时目录
        tempDir = new File(System.getProperty("java.io.tmpdir"), "excel_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
    }

    /**
     * 测试重复商品编号的Excel解析功能
     * 验证：
     * 1. 重复的商品编号都能被解析
     * 2. 每个重复条目都保持独立性
     * 3. 返回的List包含所有条目
     */
    @Test
    public void testDuplicateProductCodeParsing() throws Exception {
        // 创建包含重复商品编号的测试Excel文件
        File testExcelFile = createTestExcelWithDuplicates();
        
        // 解析Excel文件
        List<ExcelRowData> result = excelService.parseExcelFileToRowDataMap(testExcelFile.getAbsolutePath());
        
        // 验证结果
        assertNotNull("解析结果不应为null", result);
        assertEquals("应该解析出5条记录（包括重复的商品编号）", 5, result.size());

        // 验证重复商品编号的独立性
        long prod001Count = result.stream()
            .filter(data -> "PROD001".equals(data.getProductCode()))
            .count();
        assertEquals("商品编号PROD001应该有3条记录", 3L, prod001Count);

        long prod002Count = result.stream()
            .filter(data -> "PROD002".equals(data.getProductCode()))
            .count();
        assertEquals("商品编号PROD002应该有2条记录", 2L, prod002Count);

        // 验证每个条目的独立性（不同的分类信息）
        List<ExcelRowData> prod001Records = result.stream()
            .filter(data -> "PROD001".equals(data.getProductCode()))
            .collect(Collectors.toList());

        assertEquals("分类A", prod001Records.get(0).getCategory());
        assertEquals("分类B", prod001Records.get(1).getCategory());
        assertEquals("分类C", prod001Records.get(2).getCategory());
        
        System.out.println("=== 重复商品编号解析测试通过 ===");
        System.out.println("总记录数: " + result.size());
        System.out.println("PROD001记录数: " + prod001Count);
        System.out.println("PROD002记录数: " + prod002Count);
        
        // 打印所有记录详情
        for (int i = 0; i < result.size(); i++) {
            ExcelRowData data = result.get(i);
            System.out.println(String.format("记录%d: 商品编号=%s, 分类=%s, 订单号=%s", 
                i + 1, data.getProductCode(), data.getCategory(), data.getOrderNumber()));
        }
    }

    /**
     * 创建包含重复商品编号的测试Excel文件
     */
    private File createTestExcelWithDuplicates() throws IOException {
        File excelFile = new File(tempDir, "test_duplicate_products.xlsx");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("测试数据");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"商品编号", "分类", "订单号", "面料", "客户名", "市场", "套数", "SO日期", 
                               "大货日期", "工厂编号", "工厂", "描稿公司", "描稿人员", "备注", "理单员", "排序"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 创建测试数据（包含重复商品编号）
            String[][] testData = {
                {"PROD001", "分类A", "ORDER001", "棉质", "客户A", "市场A", "100", "2025-01-01", "2025-02-01", "F001", "工厂A", "设计公司A", "设计师A", "备注A", "理单员A", "1"},
                {"PROD001", "分类B", "ORDER002", "丝质", "客户B", "市场B", "200", "2025-01-02", "2025-02-02", "F002", "工厂B", "设计公司B", "设计师B", "备注B", "理单员B", "2"},
                {"PROD002", "分类X", "ORDER003", "麻质", "客户X", "市场X", "150", "2025-01-03", "2025-02-03", "F003", "工厂X", "设计公司X", "设计师X", "备注X", "理单员X", "3"},
                {"PROD001", "分类C", "ORDER004", "毛质", "客户C", "市场C", "300", "2025-01-04", "2025-02-04", "F004", "工厂C", "设计公司C", "设计师C", "备注C", "理单员C", "4"},
                {"PROD002", "分类Y", "ORDER005", "混纺", "客户Y", "市场Y", "250", "2025-01-05", "2025-02-05", "F005", "工厂Y", "设计公司Y", "设计师Y", "备注Y", "理单员Y", "5"}
            };
            
            for (int i = 0; i < testData.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < testData[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(testData[i][j]);
                }
            }
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        return excelFile;
    }
}
