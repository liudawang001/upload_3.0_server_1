package com.mls.upload.server.service;

import com.mls.upload.server.entity.ImageUpload;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.lang.reflect.Method;

/**
 * 文件命名逻辑测试
 */
public class FileNamingTest {

    /**
     * 测试花型图文件命名逻辑
     */
    @Test
    public void testFlowerImageNaming() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试花型图命名
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image";
        String filename = "test_flower.jpg";
        String imageType = ImageUpload.IMAGE_TYPE_FLOWER; // "1"
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 花型图命名测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (花型图)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("预期结果: " + targetPath + File.separator + filename);
        System.out.println("是否保持原名: " + result.equals(targetPath + File.separator + filename));
        System.out.println();
    }

    /**
     * 测试配色图文件命名逻辑
     */
    @Test
    public void testColorImageNaming() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试配色图命名
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image/color_scheme";
        String filename = "test_color.jpg";
        String imageType = ImageUpload.IMAGE_TYPE_COLOR; // "2"
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 配色图命名测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (配色图)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("是否包含时间戳: " + !result.equals(targetPath + File.separator + filename));
        System.out.println("文件名格式: " + new File(result).getName());
        System.out.println();
    }

    /**
     * 测试多次调用配色图命名的唯一性
     */
    @Test
    public void testColorImageUniqueness() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image/color_scheme";
        String filename = "duplicate_test.jpg";
        String imageType = ImageUpload.IMAGE_TYPE_COLOR;
        
        System.out.println("=== 配色图唯一性测试 ===");
        System.out.println("原始文件名: " + filename);
        
        // 生成3个文件名，验证唯一性
        for (int i = 1; i <= 3; i++) {
            String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
            System.out.println("第" + i + "次生成: " + new File(result).getName());
            
            // 稍微延迟确保时间戳不同
            Thread.sleep(1000);
        }
        System.out.println();
    }

    /**
     * 测试路径获取逻辑
     */
    @Test
    public void testPathGeneration() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method getTargetPathMethod = FileService.class.getDeclaredMethod("getTargetPath", String.class);
        getTargetPathMethod.setAccessible(true);
        
        System.out.println("=== 路径获取测试 ===");
        
        // 测试花型图路径
        String flowerPath = (String) getTargetPathMethod.invoke(fileService, ImageUpload.IMAGE_TYPE_FLOWER);
        System.out.println("花型图路径 (类型=" + ImageUpload.IMAGE_TYPE_FLOWER + "): " + flowerPath);
        
        // 测试配色图路径
        String colorPath = (String) getTargetPathMethod.invoke(fileService, ImageUpload.IMAGE_TYPE_COLOR);
        System.out.println("配色图路径 (类型=" + ImageUpload.IMAGE_TYPE_COLOR + "): " + colorPath);
        
        // 测试无效类型
        String invalidPath = (String) getTargetPathMethod.invoke(fileService, "3");
        System.out.println("无效类型路径 (类型=3): " + invalidPath);
        System.out.println();
    }
}
