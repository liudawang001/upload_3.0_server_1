package com.mls.upload.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Base64编码工具类
 * 处理图片文件的Base64编码和解码
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class Base64Util {

    private static final Logger logger = LoggerFactory.getLogger(Base64Util.class);

    /**
     * 将字节数组编码为Base64字符串
     *
     * @param data 字节数组
     * @return Base64编码的字符串
     */
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return Base64.getEncoder().encodeToString(data);
        } catch (Exception e) {
            logger.error("Base64编码失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将Base64字符串解码为字节数组
     *
     * @param base64String Base64编码的字符串
     * @return 解码后的字节数组
     */
    public static byte[] decode(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }

        try {
            // 移除可能的数据URL前缀（如：data:image/jpeg;base64,）
            String cleanBase64 = cleanBase64String(base64String);
            return Base64.getDecoder().decode(cleanBase64);
        } catch (Exception e) {
            logger.error("Base64解码失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将文件编码为Base64字符串
     *
     * @param filePath 文件路径
     * @return Base64编码的字符串
     */
    public static String encodeFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("文件路径为空");
            return null;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.warn("文件不存在或不是文件: {}", filePath);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            fis.read(fileData);
            return encode(fileData);
        } catch (IOException e) {
            logger.error("读取文件失败: path={}, error={}", filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将Base64字符串解码并保存为文件
     *
     * @param base64String Base64编码的字符串
     * @param filePath 目标文件路径
     * @return 保存是否成功
     */
    public static boolean decodeToFile(String base64String, String filePath) {
        if (base64String == null || base64String.trim().isEmpty()) {
            logger.warn("Base64字符串为空");
            return false;
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("文件路径为空");
            return false;
        }

        byte[] data = decode(base64String);
        if (data == null) {
            logger.warn("Base64解码失败");
            return false;
        }

        return FileUtil.writeFile(filePath, data);
    }

    /**
     * 检查字符串是否为有效的Base64格式
     *
     * @param base64String 待检查的字符串
     * @return 是否为有效的Base64格式
     */
    public static boolean isValidBase64(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return false;
        }

        try {
            String cleanBase64 = cleanBase64String(base64String);
            Base64.getDecoder().decode(cleanBase64);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理Base64字符串，移除数据URL前缀
     *
     * @param base64String 原始Base64字符串
     * @return 清理后的Base64字符串
     */
    public static String cleanBase64String(String base64String) {
        if (base64String == null) {
            return null;
        }

        // 移除数据URL前缀（如：data:image/jpeg;base64,）
        if (base64String.contains(",")) {
            return base64String.substring(base64String.indexOf(",") + 1);
        }

        return base64String.trim();
    }

    /**
     * 获取Base64字符串的MIME类型
     *
     * @param base64String 包含数据URL前缀的Base64字符串
     * @return MIME类型，如果无法识别则返回null
     */
    public static String getMimeType(String base64String) {
        if (base64String == null || !base64String.startsWith("data:")) {
            return null;
        }

        try {
            int semicolonIndex = base64String.indexOf(";");
            if (semicolonIndex > 5) { // "data:".length() = 5
                return base64String.substring(5, semicolonIndex);
            }
        } catch (Exception e) {
            logger.warn("解析MIME类型失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 创建数据URL格式的Base64字符串
     *
     * @param data 字节数组
     * @param mimeType MIME类型
     * @return 数据URL格式的Base64字符串
     */
    public static String createDataUrl(byte[] data, String mimeType) {
        if (data == null || data.length == 0) {
            return null;
        }

        String base64 = encode(data);
        if (base64 == null) {
            return null;
        }

        String mime = mimeType != null ? mimeType : "application/octet-stream";
        return "data:" + mime + ";base64," + base64;
    }

    /**
     * 估算Base64编码后的大小
     *
     * @param originalSize 原始数据大小（字节）
     * @return Base64编码后的大小（字节）
     */
    public static long estimateBase64Size(long originalSize) {
        // Base64编码会增加约33%的大小
        return (originalSize * 4 + 2) / 3;
    }

    /**
     * 估算Base64解码后的大小
     *
     * @param base64Size Base64编码的大小（字节）
     * @return 解码后的大小（字节）
     */
    public static long estimateDecodedSize(long base64Size) {
        // Base64解码会减少约25%的大小
        return (base64Size * 3) / 4;
    }
}
