package com.mls.upload.server.service;

import com.mls.upload.server.dto.UploadRequest;
import com.mls.upload.server.entity.PicInfo;
import com.mls.upload.server.mapper.PicInfoMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 花型图Excel数据处理测试
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowerImageExcelDataTest {

    @Mock
    private PicInfoMapper picInfoMapper;

    @InjectMocks
    private FileService fileService;

    /**
     * 测试花型图上传时Excel数据同步插入pic_info表
     */
    @Test
    public void testFlowerImageWithExcelData() throws Exception {
        // 准备测试数据
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setFilename("test_flower.jpg");
        uploadRequest.setImageType("1"); // 花型图
        uploadRequest.setUsername("admin");

        // 准备Excel数据
        Map<String, Object> excelData = new HashMap<>();
        excelData.put("field01", "TEST001");      // 商品编号
        excelData.put("field02", "花型图分类");     // 分类
        excelData.put("field03", "ORDER001");     // 订单号
        excelData.put("field04", "棉质");          // 面料
        excelData.put("field05", "测试客户");       // 客户名
        excelData.put("field06", "国内市场");       // 市场
        excelData.put("field07", "100");          // 套数
        excelData.put("field08", "2025-01-01");   // SO日期
        excelData.put("field09", "2025-02-01");   // 大货日期
        excelData.put("field10", "F001");         // 工厂编号
        excelData.put("field11", "测试工厂");       // 工厂
        excelData.put("field12", "设计公司A");      // 描稿公司
        excelData.put("field13", "张三");          // 描稿人员
        excelData.put("field14", "测试备注");       // 备注
        excelData.put("field15", "李四");          // 理单员
        excelData.put("field16", "1");            // 排序

        uploadRequest.setExcelData(excelData);

        // Mock数据库操作
        when(picInfoMapper.insert(any(PicInfo.class))).thenReturn(1);

        // 调用私有方法进行测试
        try {
            java.lang.reflect.Method method = FileService.class.getDeclaredMethod(
                "createPicInfoFromExcelData", UploadRequest.class, String.class);
            method.setAccessible(true);
            method.invoke(fileService, uploadRequest, "/test/path/test_flower.jpg");

            // 验证数据库插入操作被调用
            verify(picInfoMapper, times(1)).insert(any(PicInfo.class));

            System.out.println("✅ 花型图Excel数据处理测试通过");

        } catch (Exception e) {
            System.err.println("❌ 花型图Excel数据处理测试失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 测试filename自动填充逻辑
     */
    @Test
    public void testFilenameAutoFill() throws Exception {
        // 准备测试数据
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setFilename("original_name.jpg");
        uploadRequest.setImageType("1"); // 花型图
        uploadRequest.setUsername("admin");

        // 准备Excel数据，包含商品编号
        Map<String, Object> excelData = new HashMap<>();
        excelData.put("field01", "PRODUCT123"); // 商品编号

        uploadRequest.setExcelData(excelData);

        // Mock数据库操作，捕获插入的PicInfo对象
        when(picInfoMapper.insert(any(PicInfo.class))).thenAnswer(invocation -> {
            PicInfo picInfo = invocation.getArgument(0);
            
            // 验证filename是否按照"商品编号+.jpg"的格式填充
            String expectedFilename = "PRODUCT123.jpg";
            if (!expectedFilename.equals(picInfo.getFilename())) {
                throw new AssertionError("filename填充错误，期望: " + expectedFilename + 
                                       ", 实际: " + picInfo.getFilename());
            }
            
            // 验证商品编号字段
            if (!"PRODUCT123".equals(picInfo.getField01())) {
                throw new AssertionError("商品编号字段错误，期望: PRODUCT123, 实际: " + picInfo.getField01());
            }
            
            System.out.println("✅ filename自动填充验证通过: " + picInfo.getFilename());
            return 1;
        });

        // 调用私有方法进行测试
        try {
            java.lang.reflect.Method method = FileService.class.getDeclaredMethod(
                "createPicInfoFromExcelData", UploadRequest.class, String.class);
            method.setAccessible(true);
            method.invoke(fileService, uploadRequest, "/test/path/original_name.jpg");

            // 验证数据库插入操作被调用
            verify(picInfoMapper, times(1)).insert(any(PicInfo.class));

            System.out.println("✅ filename自动填充逻辑测试通过");

        } catch (Exception e) {
            System.err.println("❌ filename自动填充逻辑测试失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 测试配色图不处理Excel数据
     */
    @Test
    public void testColorImageSkipsExcelData() throws Exception {
        // 准备测试数据
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setFilename("test_color.jpg");
        uploadRequest.setImageType("2"); // 配色图
        uploadRequest.setUsername("admin");

        // 准备Excel数据
        Map<String, Object> excelData = new HashMap<>();
        excelData.put("field01", "TEST002");
        uploadRequest.setExcelData(excelData);

        // 模拟上传处理
        try {
            // 配色图不应该调用createPicInfoFromExcelData方法
            // 这里我们通过检查isFlowerImage()方法来验证
            boolean isFlowerImage = uploadRequest.isFlowerImage();
            if (isFlowerImage) {
                throw new AssertionError("配色图不应该被识别为花型图");
            }

            // 验证配色图标识
            boolean isColorImage = uploadRequest.isColorImage();
            if (!isColorImage) {
                throw new AssertionError("配色图应该被正确识别");
            }

            System.out.println("✅ 配色图跳过Excel数据处理测试通过");

        } catch (Exception e) {
            System.err.println("❌ 配色图跳过Excel数据处理测试失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 测试空Excel数据处理
     */
    @Test
    public void testEmptyExcelData() throws Exception {
        // 准备测试数据
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setFilename("test_flower.jpg");
        uploadRequest.setImageType("1"); // 花型图
        uploadRequest.setUsername("admin");
        uploadRequest.setExcelData(null); // 空Excel数据

        // 调用私有方法进行测试
        try {
            java.lang.reflect.Method method = FileService.class.getDeclaredMethod(
                "createPicInfoFromExcelData", UploadRequest.class, String.class);
            method.setAccessible(true);
            method.invoke(fileService, uploadRequest, "/test/path/test_flower.jpg");

            // 验证数据库插入操作不被调用
            verify(picInfoMapper, never()).insert(any(PicInfo.class));

            System.out.println("✅ 空Excel数据处理测试通过");

        } catch (Exception e) {
            System.err.println("❌ 空Excel数据处理测试失败: " + e.getMessage());
            throw e;
        }
    }
}
