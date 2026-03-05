package com.mls.upload.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /**
     * 写入字节数组到文件
     *
     * @param filePath 文件路径
     * @param data 字节数组
     * @return 写入是否成功
     */
    public static boolean writeFile(String filePath, byte[] data) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("文件路径为空");
            return false;
        }

        if (data == null || data.length == 0) {
            logger.warn("数据为空");
            return false;
        }

        try {
            // 确保目录存在
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.error("创建目录失败: {}", parentDir.getAbsolutePath());
                    return false;
                }
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
                fos.flush();
            }

            logger.info("文件写入成功: path={}, size={}", filePath, data.length);
            return true;

        } catch (IOException e) {
            logger.error("文件写入失败: path={}, error={}", filePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 读取文件为字节数组
     *
     * @param filePath 文件路径
     * @return 文件内容字节数组
     */
    public static byte[] readFile(String filePath) {
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
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            logger.info("文件读取成功: path={}, size={}", filePath, data.length);
            return data;
        } catch (IOException e) {
            logger.error("文件读取失败: path={}, error={}", filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public static boolean exists(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 删除是否成功
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("文件路径为空");
            return false;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("文件不存在: {}", filePath);
            return false;
        }

        try {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("文件删除成功: {}", filePath);
            } else {
                logger.warn("文件删除失败: {}", filePath);
            }
            return deleted;
        } catch (Exception e) {
            logger.error("文件删除异常: path={}, error={}", filePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return 创建是否成功
     */
    public static boolean createDirectory(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            logger.warn("目录路径为空");
            return false;
        }

        File dir = new File(dirPath);
        if (dir.exists()) {
            return dir.isDirectory();
        }

        try {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("目录创建成功: {}", dirPath);
            } else {
                logger.warn("目录创建失败: {}", dirPath);
            }
            return created;
        } catch (Exception e) {
            logger.error("目录创建异常: path={}, error={}", dirPath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节），如果文件不存在返回-1
     */
    public static long getFileSize(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return -1;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return -1;
        }

        return file.length();
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名（不包含点号），如果没有扩展名返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param filename 文件名
     * @return 不带扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }

        return filename;
    }

    /**
     * 生成带时间戳的唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 带时间戳的唯一文件名
     */
    public static String generateUniqueFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "file_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        String nameWithoutExt = getFileNameWithoutExtension(originalFilename);
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if (!extension.isEmpty()) {
            return nameWithoutExt + "_" + timestamp + "." + extension;
        } else {
            return nameWithoutExt + "_" + timestamp;
        }
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 复制是否成功
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        if (sourcePath == null || sourcePath.trim().isEmpty() ||
            targetPath == null || targetPath.trim().isEmpty()) {
            logger.warn("源文件路径或目标文件路径为空");
            return false;
        }

        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            // 确保目标目录存在
            File targetDir = target.getParent().toFile();
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logger.error("创建目标目录失败: {}", targetDir.getAbsolutePath());
                return false;
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件复制成功: {} -> {}", sourcePath, targetPath);
            return true;

        } catch (Exception e) {
            logger.error("文件复制失败: {} -> {}, error={}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 移动是否成功
     */
    public static boolean moveFile(String sourcePath, String targetPath) {
        if (sourcePath == null || sourcePath.trim().isEmpty() ||
            targetPath == null || targetPath.trim().isEmpty()) {
            logger.warn("源文件路径或目标文件路径为空");
            return false;
        }

        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            // 确保目标目录存在
            File targetDir = target.getParent().toFile();
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                logger.error("创建目标目录失败: {}", targetDir.getAbsolutePath());
                return false;
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件移动成功: {} -> {}", sourcePath, targetPath);
            return true;

        } catch (Exception e) {
            logger.error("文件移动失败: {} -> {}, error={}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 列出目录下的所有文件
     *
     * @param dirPath 目录路径
     * @param recursive 是否递归查找子目录
     * @return 文件路径列表
     */
    public static List<String> listFiles(String dirPath, boolean recursive) {
        List<String> fileList = new ArrayList<>();

        if (dirPath == null || dirPath.trim().isEmpty()) {
            logger.warn("目录路径为空");
            return fileList;
        }

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("目录不存在或不是目录: {}", dirPath);
            return fileList;
        }

        try {
            listFilesRecursive(dir, fileList, recursive);
        } catch (Exception e) {
            logger.error("列出文件异常: path={}, error={}", dirPath, e.getMessage(), e);
        }

        return fileList;
    }

    /**
     * 递归列出文件
     *
     * @param dir 目录
     * @param fileList 文件列表
     * @param recursive 是否递归
     */
    private static void listFilesRecursive(File dir, List<String> fileList, boolean recursive) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                fileList.add(file.getAbsolutePath());
            } else if (file.isDirectory() && recursive) {
                listFilesRecursive(file, fileList, recursive);
            }
        }
    }

    /**
     * 检查文件是否为图片格式
     *
     * @param filename 文件名
     * @return 是否为图片格式
     */
    public static boolean isImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename);
        return extension.equals("jpg") || extension.equals("jpeg") ||
               extension.equals("png") || extension.equals("gif") ||
               extension.equals("bmp") || extension.equals("webp");
    }
}
