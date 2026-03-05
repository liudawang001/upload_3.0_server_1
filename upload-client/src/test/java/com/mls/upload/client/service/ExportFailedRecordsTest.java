package com.mls.upload.client.service;

import com.mls.upload.client.model.vo.LogEntry;
import com.mls.upload.client.model.entity.ExcelRowData;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出失败记录功能测试
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ExportFailedRecordsTest {

    private LogService logService;
    private List<LogEntry> testEntries;

    @Before
    public void setUp() {
        logService = new LogService();
        testEntries = createTestLogEntries();
    }

    /**
     * 测试筛选失败记录功能
     */
    @Test
    public void testFilterFailedEntries() {
        // 执行筛选
        List<LogEntry> failedEntries = logService.filterFailedEntries(testEntries);

        // 验证结果
        assertEquals("应该筛选出4条失败记录", 4, failedEntries.size());

        // 验证包含上传失败的记录
        assertTrue("应该包含上传失败的记录", 
            failedEntries.stream().anyMatch(entry -> 
                "PROD001".equals(entry.getProductCode()) && "失败".equals(entry.getUploadStatus())));

        // 验证包含比对失败的记录
        assertTrue("应该包含不匹配的记录", 
            failedEntries.stream().anyMatch(entry -> 
                "PROD002".equals(entry.getProductCode()) && "不匹配".equals(entry.getMatchStatus())));

        assertTrue("应该包含缺少图片的记录", 
            failedEntries.stream().anyMatch(entry -> 
                "PROD003".equals(entry.getProductCode()) && "缺少图片".equals(entry.getMatchStatus())));

        assertTrue("应该包含缺少数据的记录", 
            failedEntries.stream().anyMatch(entry -> 
                "PROD004".equals(entry.getProductCode()) && "缺少数据".equals(entry.getMatchStatus())));

        // 验证不包含成功的记录
        assertFalse("不应该包含成功的记录", 
            failedEntries.stream().anyMatch(entry -> 
                "PROD005".equals(entry.getProductCode())));
    }

    /**
     * 测试空列表处理
     */
    @Test
    public void testFilterFailedEntriesWithEmptyList() {
        List<LogEntry> emptyList = new ArrayList<>();
        List<LogEntry> result = logService.filterFailedEntries(emptyList);
        
        assertNotNull("结果不应该为null", result);
        assertTrue("结果应该为空列表", result.isEmpty());
    }

    /**
     * 测试null输入处理
     */
    @Test
    public void testFilterFailedEntriesWithNull() {
        List<LogEntry> result = logService.filterFailedEntries(null);
        
        assertNotNull("结果不应该为null", result);
        assertTrue("结果应该为空列表", result.isEmpty());
    }

    /**
     * 测试Excel标题行提取
     */
    @Test
    public void testExtractExcelHeaders() {
        List<String> headers = logService.extractExcelHeaders();
        
        assertNotNull("标题行不应该为null", headers);
        assertEquals("应该有16列标题", 16, headers.size());
        assertEquals("第一列应该是商品编号", "商品编号", headers.get(0));
        assertEquals("最后一列应该是排序", "排序", headers.get(15));
    }

    /**
     * 创建测试用的日志条目
     */
    private List<LogEntry> createTestLogEntries() {
        List<LogEntry> entries = new ArrayList<>();

        // 上传失败的记录
        LogEntry entry1 = new LogEntry();
        entry1.setProductCode("PROD001");
        entry1.setUploadStatus("失败");
        entry1.setMatchStatus("比对成功");
        entry1.setExcelRowData(createTestExcelRowData("PROD001"));
        entries.add(entry1);

        // 比对不匹配的记录
        LogEntry entry2 = new LogEntry();
        entry2.setProductCode("PROD002");
        entry2.setUploadStatus("待上传");
        entry2.setMatchStatus("不匹配");
        entry2.setExcelRowData(createTestExcelRowData("PROD002"));
        entries.add(entry2);

        // 缺少图片的记录
        LogEntry entry3 = new LogEntry();
        entry3.setProductCode("PROD003");
        entry3.setUploadStatus("待上传");
        entry3.setMatchStatus("缺少图片");
        entry3.setExcelRowData(createTestExcelRowData("PROD003"));
        entries.add(entry3);

        // 缺少数据的记录
        LogEntry entry4 = new LogEntry();
        entry4.setProductCode("PROD004");
        entry4.setUploadStatus("待上传");
        entry4.setMatchStatus("缺少数据");
        entry4.setExcelRowData(createTestExcelRowData("PROD004"));
        entries.add(entry4);

        // 成功的记录（不应该被筛选出来）
        LogEntry entry5 = new LogEntry();
        entry5.setProductCode("PROD005");
        entry5.setUploadStatus("成功");
        entry5.setMatchStatus("比对成功");
        entry5.setExcelRowData(createTestExcelRowData("PROD005"));
        entries.add(entry5);

        return entries;
    }

    /**
     * 创建测试用的Excel行数据
     */
    private ExcelRowData createTestExcelRowData(String productCode) {
        ExcelRowData rowData = new ExcelRowData();
        rowData.setProductCode(productCode);
        rowData.setCategory("测试分类");
        rowData.setOrderNumber("ORDER001");
        rowData.setFabric("测试面料");
        rowData.setCustomerName("测试客户");
        rowData.setMarket("测试市场");
        rowData.setSetCount("100");
        rowData.setSoDate("2025-01-01");
        rowData.setBulkDate("2025-02-01");
        rowData.setFactoryCode("FAC001");
        rowData.setFactory("测试工厂");
        rowData.setDesignCompany("测试设计公司");
        rowData.setDesigner("测试设计师");
        rowData.setRemark("测试备注");
        rowData.setMerchandiser("测试理单员");
        rowData.setSortOrder("1");
        return rowData;
    }
}
