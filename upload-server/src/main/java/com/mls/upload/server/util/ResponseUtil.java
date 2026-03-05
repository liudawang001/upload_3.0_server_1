package com.mls.upload.server.util;

import com.mls.upload.server.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 响应工具类
 * 处理API响应的统一格式化
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class ResponseUtil {

    private static final Logger logger = LoggerFactory.getLogger(ResponseUtil.class);

    /**
     * 创建成功响应
     *
     * @param message 响应消息
     * @return 成功响应
     */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.success(message);
    }

    /**
     * 创建成功响应（带数据）
     *
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.success(message, data);
    }

    /**
     * 创建错误响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.error(message);
    }

    /**
     * 创建未授权响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return ApiResponse.unauthorized(message);
    }

    /**
     * 创建资源未找到响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 资源未找到响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return ApiResponse.notFound(message);
    }

    /**
     * 创建参数错误响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 参数错误响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return ApiResponse.validationError(message);
    }

    /**
     * 根据业务结果创建响应
     *
     * @param success 业务是否成功
     * @param successMessage 成功消息
     * @param errorMessage 错误消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应
     */
    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> result(boolean success, String successMessage,
                                           String errorMessage, T data) {
        if (success) {
            return data != null ? success(successMessage, data) : (ApiResponse<T>) success(successMessage);
        } else {
            return error(errorMessage);
        }
    }

    /**
     * 根据业务结果创建响应（无数据）
     *
     * @param success 业务是否成功
     * @param successMessage 成功消息
     * @param errorMessage 错误消息
     * @param <T> 数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> result(boolean success, String successMessage, String errorMessage) {
        return result(success, successMessage, errorMessage, null);
    }

    /**
     * 处理异常并创建错误响应
     *
     * @param e 异常
     * @param operation 操作描述
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> handleException(Exception e, String operation) {
        String errorMessage = String.format("%s异常: %s", operation, e.getMessage());
        logger.error(errorMessage, e);
        return error(errorMessage);
    }

    /**
     * 创建分页响应数据
     *
     * @param data 数据列表
     * @param total 总数量
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return 分页响应数据
     */
    public static <T> PageResponse<T> createPageResponse(java.util.List<T> data, long total, int page, int size) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setData(data);
        pageResponse.setTotal(total);
        pageResponse.setPage(page);
        pageResponse.setSize(size);
        pageResponse.setTotalPages((int) Math.ceil((double) total / size));
        pageResponse.setHasNext(page < pageResponse.getTotalPages());
        pageResponse.setHasPrevious(page > 1);
        return pageResponse;
    }

    /**
     * 创建带时间戳的响应
     *
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 带时间戳的响应
     */
    public static <T> TimestampResponse<T> createTimestampResponse(String message, T data) {
        TimestampResponse<T> response = new TimestampResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return response;
    }

    /**
     * 分页响应数据类
     */
    public static class PageResponse<T> {
        private java.util.List<T> data;
        private long total;
        private int page;
        private int size;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        // Getters and Setters
        public java.util.List<T> getData() { return data; }
        public void setData(java.util.List<T> data) { this.data = data; }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }

    /**
     * 带时间戳的响应数据类
     */
    public static class TimestampResponse<T> {
        private int code;
        private String message;
        private T data;
        private String timestamp;

        // Getters and Setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
