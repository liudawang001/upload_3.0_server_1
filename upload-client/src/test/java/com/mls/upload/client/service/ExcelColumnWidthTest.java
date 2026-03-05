package com.mls.upload.client.service;

import com.mls.upload.client.model.entity.ExcelRowData;
import com.mls.upload.client.model.vo.LogEntry;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Excel列宽调整功能测试
 */
public class ExcelColumnWidthTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelColumnWidthTest.class);
    
    private ExcelService excelService;
    private List<String> headerColumns;
    private List<LogEntry> testEntries;
    
    @Before
    public void setUp() {
        excelService = new ExcelService();
        
        // 设置16列标题
        headerColumns = Arrays.asList(
            "商品编号", "分类", "订单号", "面料", "客户名", "市场", "套数", "SO日期",
            "大货日期", "工厂编号", "工厂", "描稿公司", "描稿人员", "备注", "理单员", "排序"
        );
        
        // 创建测试数据 - 包含长短不一的内容
        testEntries = createTestData();
    }
    
    @Test
    public void testExcelColumnWidthAdjustment() throws Exception {
        logger.info("开始测试Excel列宽调整功能");
        
        String testFilePath = "target/test-column-width.xlsx";
        
        try {
            // 导出Excel文件
            excelService.exportFailedRecordsToExcel(testEntries, headerColumns, testFilePath);
            
            // 验证文件是否创建成功
            File exportedFile = new File(testFilePath);
            assertTrue("导出的Excel文件应该存在", exportedFile.exists());
            assertTrue("导出的Excel文件大小应该大于0", exportedFile.length() > 0);
            
            // 验证Excel文件内容和列宽
            verifyExcelColumnWidths(testFilePath);
            
            logger.info("Excel列宽调整功能测试通过");
            
        } finally {
            // 清理测试文件
            File testFile = new File(testFilePath);
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }
    
    private void verifyExcelColumnWidths(String filePath) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new java.io.FileInputStream(filePath))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 验证每列都有合理的宽度设置
            for (int i = 0; i < headerColumns.size(); i++) {
                int columnWidth = sheet.getColumnWidth(i);
                
                // 列宽应该大于最小值（至少能显示标题）
                int minExpectedWidth = headerColumns.get(i).length() * 256;
                assertTrue(String.format("列 %d (%s) 宽度 %d 应该大于最小宽度 %d", 
                          i, headerColumns.get(i), columnWidth, minExpectedWidth),
                          columnWidth >= minExpectedWidth);
                
                // 列宽不应该超过最大限制
                int maxExpectedWidth = 50 * 256; // 50个字符
                assertTrue(String.format("列 %d (%s) 宽度 %d 不应该超过最大宽度 %d", 
                          i, headerColumns.get(i), columnWidth, maxExpectedWidth),
                          columnWidth <= maxExpectedWidth);
                
                logger.debug("列 {} ({}) 宽度: {} ({}个字符)", 
                           i, headerColumns.get(i), columnWidth, columnWidth / 256);
            }
            
            // 验证数据行数
            int dataRows = sheet.getLastRowNum();
            assertEquals("应该有正确的数据行数", testEntries.size(), dataRows);
        }
    }
    
    private List<LogEntry> createTestData() {
        LogEntry entry1 = new LogEntry();
        ExcelRowData data1 = new ExcelRowData();
        data1.setProductCode("PROD001");
        data1.setCategory("分类A");
        data1.setOrderNumber("ORDER123456789");
        data1.setFabric("棉质面料");
        data1.setCustomerName("客户名称测试");
        data1.setMarket("市场A");
        data1.setSetCount("100");
        data1.setSoDate("2025-01-01");
        data1.setBulkDate("2025-02-01");
        data1.setFactoryCode("FAC001");
        data1.setFactory("工厂名称测试有限公司");
        data1.setDesignCompany("设计公司");
        data1.setDesigner("设计师");
        data1.setRemark("这是一个很长的备注信息，用来测试列宽调整功能是否正常工作");
        data1.setMerchandiser("理单员");
        data1.setSortOrder("1");
        entry1.setExcelRowData(data1);
        entry1.setUploadStatus("失败");
        
        LogEntry entry2 = new LogEntry();
        ExcelRowData data2 = new ExcelRowData();
        data2.setProductCode("PRODUCT002VERYLONGCODE");
        data2.setCategory("B");
        data2.setOrderNumber("ORD2");
        data2.setFabric("丝绸");
        data2.setCustomerName("短名");
        data2.setMarket("市场B测试");
        data2.setSetCount("50");
        data2.setSoDate("2025-01-15");
        data2.setBulkDate("2025-02-15");
        data2.setFactoryCode("FACTORY002VERYLONGCODE");
        data2.setFactory("工厂B");
        data2.setDesignCompany("设计公司B有限责任公司");
        data2.setDesigner("设计师B");
        data2.setRemark("短备注");
        data2.setMerchandiser("理单员B测试名称");
        data2.setSortOrder("2");
        entry2.setExcelRowData(data2);
        entry2.setMatchStatus("不匹配");
        
        return Arrays.asList(entry1, entry2);
    }
}
