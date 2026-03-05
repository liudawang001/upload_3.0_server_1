package com.mls.upload.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
     * @param bytes 字节数组
     * @return Base64字符串
     */
    public static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        try {
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.error("Base64编码失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 将Base64字符串解码为字节数组
     *
     * @param base64String Base64字符串
     * @return 字节数组
     */
    public static byte[] decode(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return new byte[0];
        }

        try {
            return Base64.getDecoder().decode(base64String);
        } catch (Exception e) {
            logger.error("Base64解码失败: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    /**
     * 将文件编码为Base64字符串
     *
     * @param file 文件
     * @return Base64字符串
     * @throws IOException IO异常
     */
    public static String encodeFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("无效的文件: " + (file != null ? file.getPath() : "null"));
        }

        logger.debug("开始编码文件: {}, 大小: {} bytes", file.getName(), file.length());

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String base64String = encode(fileBytes);

        logger.debug("文件编码完成: {}, Base64长度: {}", file.getName(), base64String.length());
        return base64String;
    }

    /**
     * 将Base64字符串解码并保存为文件
     *
     * @param base64String Base64字符串
     * @param outputFile 输出文件
     * @throws IOException IO异常
     */
    public static void decodeToFile(String base64String, File outputFile) throws IOException {
        if (base64String == null || base64String.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64字符串不能为空");
        }

        if (outputFile == null) {
            throw new IllegalArgumentException("输出文件不能为空");
        }

        logger.debug("开始解码Base64到文件: {}", outputFile.getName());

        byte[] decodedBytes = decode(base64String);
        if (decodedBytes.length == 0) {
            throw new IOException("Base64解码失败，结果为空");
        }

        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("无法创建目录: " + parentDir.getPath());
            }
        }

        Files.write(outputFile.toPath(), decodedBytes);

        logger.debug("Base64解码到文件完成: {}, 大小: {} bytes", outputFile.getName(), decodedBytes.length);
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
            // 尝试解码，如果成功则说明格式有效
            Base64.getDecoder().decode(base64String);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Base64字符串的原始数据大小（字节）
     *
     * @param base64String Base64字符串
     * @return 原始数据大小，-1表示无效的Base64字符串
     */
    public static long getOriginalSize(String base64String) {
        if (!isValidBase64(base64String)) {
            return -1;
        }

        try {
            byte[] decodedBytes = decode(base64String);
            return decodedBytes.length;
        } catch (Exception e) {
            logger.error("获取Base64原始大小失败: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 计算Base64编码后的大小
     *
     * @param originalSize 原始数据大小
     * @return Base64编码后的大小
     */
    public static long calculateBase64Size(long originalSize) {
        // Base64编码会增加约33%的大小，加上填充字符
        return ((originalSize + 2) / 3) * 4;
    }
}
