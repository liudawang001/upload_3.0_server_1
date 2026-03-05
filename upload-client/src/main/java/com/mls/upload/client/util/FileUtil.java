package com.mls.upload.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件处理工具类
 * 处理文件操作相关功能
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.#");

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（包含点号）
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * 获取文件名（不包含扩展名）
     *
     * @param fileName 文件名
     * @return 不包含扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的大小字符串
     */
    public static String formatFileSize(long size) {
        if (size < 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return SIZE_FORMAT.format(fileSize) + " " + units[unitIndex];
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 创建目录（如果不存在）
     *
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDirectories(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.debug("创建目录: {}", dirPath);
            }
            return true;
        } catch (IOException e) {
            logger.error("创建目录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.debug("删除文件: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("删除文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否复制成功
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        if (sourcePath == null || targetPath == null) {
            return false;
        }

        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            // 确保目标目录存在
            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("复制文件: {} -> {}", sourcePath, targetPath);
            return true;
        } catch (IOException e) {
            logger.error("复制文件失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 计算文件MD5值
     *
     * @param file 文件
     * @return MD5值
     * @throws IOException IO异常
     */
    public static String calculateMD5(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("无效的文件");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new IOException("计算MD5失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取临时目录路径
     *
     * @return 临时目录路径
     */
    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * 创建临时文件
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件
     * @throws IOException IO异常
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix).toFile();
    }

    /**
     * 列出目录中的所有文件
     *
     * @param dirPath 目录路径
     * @param recursive 是否递归
     * @return 文件列表
     */
    public static List<File> listFiles(String dirPath, boolean recursive) {
        List<File> files = new ArrayList<>();

        if (dirPath == null || dirPath.trim().isEmpty()) {
            return files;
        }

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }

        listFilesRecursive(dir, files, recursive);
        return files;
    }

    /**
     * 递归列出文件
     *
     * @param dir 目录
     * @param files 文件列表
     * @param recursive 是否递归
     */
    private static void listFilesRecursive(File dir, List<File> files, boolean recursive) {
        File[] fileArray = dir.listFiles();
        if (fileArray == null) {
            return;
        }

        for (File file : fileArray) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory() && recursive) {
                listFilesRecursive(file, files, true);
            }
        }
    }

    /**
     * 清理目录（删除目录下的所有文件和子目录）
     *
     * @param dirPath 目录路径
     * @return 是否清理成功
     */
    public static boolean cleanDirectory(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return false;
            }

            Files.walk(path)
                    .filter(p -> !p.equals(path))
                    .sorted((p1, p2) -> p2.compareTo(p1)) // 先删除文件，再删除目录
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warn("删除文件失败: {}", p, e);
                        }
                    });

            logger.debug("清理目录: {}", dirPath);
            return true;
        } catch (IOException e) {
            logger.error("清理目录失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
