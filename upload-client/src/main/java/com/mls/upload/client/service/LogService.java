package com.mls.upload.client.service;

import com.mls.upload.client.model.vo.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志服务
 * 处理操作日志的记录和管理
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出日志到CSV文件
     *
     * @param logEntries 日志条目列表
     * @param filePath 导出文件路径
     * @throws IOException IO异常
     */
    public void exportLogToCsv(List<LogEntry> logEntries, String filePath) throws IOException {
        logger.info("开始导出日志到CSV文件: {}", filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            // 写入CSV头部
            writer.write("序号,商品编号,图片名称,比对状态,上传状态,进度,备注信息,时间");
            writer.newLine();

            // 写入数据行
            for (LogEntry entry : logEntries) {
                StringBuilder line = new StringBuilder();
                line.append(csvEscape(String.valueOf(entry.getIndex()))).append(",");
                line.append(csvEscape(entry.getProductCode())).append(",");
                line.append(csvEscape(entry.getFilename())).append(",");
                line.append(csvEscape(entry.getMatchStatus())).append(",");
                line.append(csvEscape(entry.getUploadStatus())).append(",");
                line.append(csvEscape(entry.getProgress())).append(",");
                line.append(csvEscape(entry.getMessage())).append(",");
                line.append(csvEscape(entry.getTime()));

                writer.write(line.toString());
                writer.newLine();
            }

            writer.flush();
        }

        logger.info("日志导出完成: 文件={}, 记录数={}", filePath, logEntries.size());
    }

    /**
     * CSV字段转义
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }

        // 如果包含逗号、双引号或换行符，需要用双引号包围，并转义内部的双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * 记录操作日志
     *
     * @param operation 操作类型
     * @param message 日志消息
     * @param details 详细信息
     */
    public void logOperation(String operation, String message, Object details) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String logMessage = String.format("[%s] %s: %s", timestamp, operation, message);

        if (details != null) {
            logMessage += " - " + details.toString();
        }

        logger.info(logMessage);
    }

    /**
     * 记录错误日志
     *
     * @param operation 操作类型
     * @param error 错误信息
     * @param exception 异常对象
     */
    public void logError(String operation, String error, Throwable exception) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String logMessage = String.format("[%s] %s ERROR: %s", timestamp, operation, error);

        if (exception != null) {
            logger.error(logMessage, exception);
        } else {
            logger.error(logMessage);
        }
    }

    /**
     * 记录警告日志
     *
     * @param operation 操作类型
     * @param warning 警告信息
     */
    public void logWarning(String operation, String warning) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String logMessage = String.format("[%s] %s WARNING: %s", timestamp, operation, warning);
        logger.warn(logMessage);
    }

    /**
     * 记录调试日志
     *
     * @param operation 操作类型
     * @param message 调试信息
     */
    public void logDebug(String operation, String message) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String logMessage = String.format("[%s] %s DEBUG: %s", timestamp, operation, message);
        logger.debug(logMessage);
    }

    /**
     * 创建日志条目
     *
     * @param index 序号
     * @param productCode 商品编号
     * @param filename 文件名
     * @param matchStatus 比对状态
     * @param uploadStatus 上传状态
     * @param progress 进度
     * @param message 消息
     * @return 日志条目
     */
    public LogEntry createLogEntry(Integer index, String productCode, String filename,
                                  String matchStatus, String uploadStatus, String progress, String message) {
        LogEntry entry = new LogEntry();
        entry.setIndex(index);
        entry.setProductCode(productCode);
        entry.setFilename(filename);
        entry.setMatchStatus(matchStatus);
        entry.setUploadStatus(uploadStatus);
        entry.setProgress(progress);
        entry.setMessage(message);
        entry.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return entry;
    }

    /**
     * 更新日志条目状态
     *
     * @param entry 日志条目
     * @param matchStatus 比对状态
     * @param uploadStatus 上传状态
     * @param progress 进度
     * @param message 消息
     */
    public void updateLogEntry(LogEntry entry, String matchStatus, String uploadStatus,
                              String progress, String message) {
        if (matchStatus != null) {
            entry.setMatchStatus(matchStatus);
        }
        if (uploadStatus != null) {
            entry.setUploadStatus(uploadStatus);
        }
        if (progress != null) {
            entry.setProgress(progress);
        }
        if (message != null) {
            entry.setMessage(message);
        }
        entry.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * 清理过期日志文件
     *
     * @param logDirectory 日志目录
     * @param maxDays 保留天数
     */
    public void cleanupOldLogs(String logDirectory, int maxDays) {
        // 这里可以实现日志文件清理逻辑
        logger.info("日志清理功能待实现: 目录={}, 保留天数={}", logDirectory, maxDays);
    }

    /**
     * 筛选失败的日志条目
     * 包括上传失败和比对不匹配的记录
     *
     * @param allEntries 所有日志条目
     * @return 失败的日志条目列表
     */
    public List<LogEntry> filterFailedEntries(List<LogEntry> allEntries) {
        if (allEntries == null || allEntries.isEmpty()) {
            logger.info("没有日志条目可筛选");
            return new ArrayList<>();
        }

        List<LogEntry> failedEntries = allEntries.stream()
            .filter(entry -> isFailedOrMismatchedEntry(entry))
            .collect(java.util.stream.Collectors.toList());

        // 统计各种失败类型的数量
        long uploadFailedCount = allEntries.stream()
            .filter(entry -> "失败".equals(entry.getUploadStatus()))
            .count();

        long matchFailedCount = allEntries.stream()
            .filter(entry -> isMatchStatusFailed(entry.getMatchStatus()))
            .count();

        logger.info("筛选失败记录完成: 总记录数={}, 失败记录数={} (上传失败: {}, 比对失败: {})",
                   allEntries.size(), failedEntries.size(), uploadFailedCount, matchFailedCount);
        return failedEntries;
    }

    /**
     * 判断是否为失败或不匹配的条目
     *
     * @param entry 日志条目
     * @return true表示是失败或不匹配的条目
     */
    private boolean isFailedOrMismatchedEntry(LogEntry entry) {
        if (entry == null) {
            return false;
        }

        // 检查上传状态是否失败
        boolean uploadFailed = "失败".equals(entry.getUploadStatus());

        // 检查比对状态是否失败
        boolean matchFailed = isMatchStatusFailed(entry.getMatchStatus());

        return uploadFailed || matchFailed;
    }

    /**
     * 判断比对状态是否为失败状态
     *
     * @param matchStatus 比对状态
     * @return true表示比对失败
     */
    private boolean isMatchStatusFailed(String matchStatus) {
        if (matchStatus == null) {
            return false;
        }

        return "不匹配".equals(matchStatus) ||
               "缺少图片".equals(matchStatus) ||
               "缺少数据".equals(matchStatus);
    }

    /**
     * 提取Excel标题行
     *
     * @return Excel标题行列表
     */
    public List<String> extractExcelHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("商品编号");
        headers.add("分类");
        headers.add("订单号");
        headers.add("面料");
        headers.add("客户名");
        headers.add("市场");
        headers.add("套数");
        headers.add("SO日期");
        headers.add("大货日期");
        headers.add("工厂编号");
        headers.add("工厂");
        headers.add("描稿公司");
        headers.add("描稿人员");
        headers.add("备注");
        headers.add("理单员");
        headers.add("排序");

        logger.debug("Excel标题行提取完成: 列数={}", headers.size());
        return headers;
    }

    /**
     * 验证Excel数据完整性
     *
     * @param entries 日志条目列表
     * @return 验证结果
     */
    public boolean validateExcelData(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            logger.warn("没有数据可验证");
            return false;
        }

        int validCount = 0;
        for (LogEntry entry : entries) {
            if (entry.getExcelRowData() != null &&
                entry.getExcelRowData().getProductCode() != null &&
                !entry.getExcelRowData().getProductCode().trim().isEmpty()) {
                validCount++;
            }
        }

        boolean isValid = validCount > 0;
        logger.info("Excel数据验证完成: 总记录数={}, 有效记录数={}, 验证结果={}",
                   entries.size(), validCount, isValid);

        return isValid;
    }
}
