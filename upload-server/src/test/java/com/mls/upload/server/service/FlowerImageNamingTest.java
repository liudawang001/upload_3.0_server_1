package com.mls.upload.server.service;

import com.mls.upload.server.entity.ImageUpload;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.lang.reflect.Method;

/**
 * 花型图文件命名逻辑测试
 */
public class FlowerImageNamingTest {

    /**
     * 测试花型图文件命名逻辑 - 保持原始文件名
     */
    @Test
    public void testFlowerImageKeepsOriginalName() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试花型图命名
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image";
        String filename = "original_flower.jpg";
        String imageType = ImageUpload.IMAGE_TYPE_FLOWER; // "1"
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 花型图命名逻辑测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (花型图)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("预期结果: " + targetPath + File.separator + filename);
        System.out.println("是否保持原名: " + result.equals(targetPath + File.separator + filename));
        System.out.println("文件名是否包含时间戳: " + result.contains("_202"));
        System.out.println();
        
        // 验证结果
        String expectedPath = targetPath + File.separator + filename;
        if (!result.equals(expectedPath)) {
            throw new AssertionError("花型图文件名应该保持不变！预期: " + expectedPath + ", 实际: " + result);
        }
        
        if (result.contains("_202")) {
            throw new AssertionError("花型图文件名不应该包含时间戳！实际路径: " + result);
        }
        
        System.out.println("✅ 花型图命名测试通过：文件名保持原始不变");
    }

    /**
     * 测试配色图文件命名逻辑 - 添加时间戳
     */
    @Test
    public void testColorImageAddsTimestamp() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试配色图命名
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image/color_scheme";
        String filename = "original_color.jpg";
        String imageType = ImageUpload.IMAGE_TYPE_COLOR; // "2"
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 配色图命名逻辑测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (配色图)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("文件名是否包含时间戳: " + result.contains("_202"));
        System.out.println("生成的文件名: " + new File(result).getName());
        System.out.println();
        
        // 验证结果
        if (!result.contains("_202")) {
            throw new AssertionError("配色图文件名应该包含时间戳！实际路径: " + result);
        }
        
        if (result.equals(targetPath + File.separator + filename)) {
            throw new AssertionError("配色图文件名不应该保持原始不变！实际路径: " + result);
        }
        
        System.out.println("✅ 配色图命名测试通过：文件名包含时间戳");
    }

    /**
     * 测试无效图片类型的默认处理
     */
    @Test
    public void testInvalidImageTypeDefaultBehavior() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试无效图片类型
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image";
        String filename = "invalid_type.jpg";
        String imageType = "3"; // 无效类型
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 无效图片类型默认处理测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (无效类型)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("文件名是否包含时间戳: " + result.contains("_202"));
        System.out.println("生成的文件名: " + new File(result).getName());
        System.out.println();
        
        // 验证结果 - 无效类型应该使用默认策略（添加时间戳）
        if (!result.contains("_202")) {
            throw new AssertionError("无效图片类型应该使用默认策略（添加时间戳）！实际路径: " + result);
        }
        
        System.out.println("✅ 无效图片类型测试通过：使用默认策略添加时间戳");
    }

    /**
     * 测试中文图片类型的处理（模拟修复前的错误情况）
     */
    @Test
    public void testChineseImageTypeHandling() throws Exception {
        FileService fileService = new FileService();
        
        // 设置路径
        ReflectionTestUtils.setField(fileService, "flowerImagePath", "G:/A_Projects/Pic_Manage/mls_image");
        ReflectionTestUtils.setField(fileService, "colorImagePath", "G:/A_Projects/Pic_Manage/mls_image/color_scheme");
        
        // 获取私有方法
        Method generateFilePathMethod = FileService.class.getDeclaredMethod("generateFilePath", String.class, String.class, String.class);
        generateFilePathMethod.setAccessible(true);
        
        // 测试中文图片类型（修复前客户端发送的格式）
        String targetPath = "G:/A_Projects/Pic_Manage/mls_image";
        String filename = "chinese_type.jpg";
        String imageType = "花型图"; // 中文类型（错误格式）
        
        String result = (String) generateFilePathMethod.invoke(fileService, targetPath, filename, imageType);
        
        System.out.println("=== 中文图片类型处理测试 ===");
        System.out.println("原始文件名: " + filename);
        System.out.println("图片类型: " + imageType + " (中文类型 - 错误格式)");
        System.out.println("目标路径: " + targetPath);
        System.out.println("生成路径: " + result);
        System.out.println("文件名是否包含时间戳: " + result.contains("_202"));
        System.out.println("生成的文件名: " + new File(result).getName());
        System.out.println();
        
        // 验证结果 - 中文类型不匹配常量，应该使用默认策略（添加时间戳）
        if (!result.contains("_202")) {
            throw new AssertionError("中文图片类型不匹配常量，应该使用默认策略（添加时间戳）！实际路径: " + result);
        }
        
        System.out.println("✅ 中文图片类型测试通过：不匹配常量时使用默认策略");
        System.out.println("⚠️  这说明修复前客户端发送中文类型会导致花型图也添加时间戳");
    }
}
