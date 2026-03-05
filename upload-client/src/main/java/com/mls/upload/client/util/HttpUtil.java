package com.mls.upload.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP通信工具类
 * 封装OkHttp客户端，处理与服务端的通信
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.get("application/x-www-form-urlencoded");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public HttpUtil() {
        // 创建OkHttp客户端
        this.client = new OkHttpClient.Builder()
                .connectTimeout(ConfigUtil.getServerTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(ConfigUtil.getServerTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(ConfigUtil.getServerTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();

        // 创建JSON映射器
        this.objectMapper = new ObjectMapper();

        logger.info("HTTP工具类初始化完成，超时时间: {}ms", ConfigUtil.getServerTimeout());
    }

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应结果
     */
    public Map<String, Object> get(String url) {
        return get(url, null);
    }

    /**
     * 发送GET请求（带请求头）
     *
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String url, Map<String, String> headers) {
        logger.debug("发送GET请求: {}", url);

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);

            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.debug("GET请求响应: code={}, body={}", response.code(), responseBody);

                if (response.isSuccessful()) {
                    return objectMapper.readValue(responseBody, Map.class);
                } else {
                    logger.warn("GET请求失败: code={}, message={}", response.code(), response.message());
                    return createErrorResponse(response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("GET请求异常: url={}, error={}", url, e.getMessage(), e);
            return createErrorResponse(500, "网络请求异常: " + e.getMessage());
        }
    }

    /**
     * 发送POST JSON请求
     *
     * @param url 请求URL
     * @param data 请求数据
     * @return 响应结果
     */
    public Map<String, Object> postJson(String url, Object data) {
        return postJson(url, data, null);
    }

    /**
     * 发送POST JSON请求（带请求头）
     *
     * @param url 请求URL
     * @param data 请求数据
     * @param headers 请求头
     * @return 响应结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> postJson(String url, Object data, Map<String, String> headers) {
        logger.debug("发送POST JSON请求: {}", url);

        try {
            String jsonData = objectMapper.writeValueAsString(data);
            RequestBody body = RequestBody.create(JSON, jsonData);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.debug("POST JSON请求响应: code={}, body={}", response.code(), responseBody);

                if (response.isSuccessful()) {
                    return objectMapper.readValue(responseBody, Map.class);
                } else {
                    logger.warn("POST JSON请求失败: code={}, message={}", response.code(), response.message());
                    return createErrorResponse(response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("POST JSON请求异常: url={}, error={}", url, e.getMessage(), e);
            return createErrorResponse(500, "网络请求异常: " + e.getMessage());
        }
    }

    /**
     * 发送POST表单请求
     *
     * @param url 请求URL
     * @param params 表单参数
     * @return 响应结果
     */
    public Map<String, Object> postForm(String url, Map<String, String> params) {
        return postForm(url, params, null);
    }

    /**
     * 发送POST表单请求（带请求头）
     *
     * @param url 请求URL
     * @param params 表单参数
     * @param headers 请求头
     * @return 响应结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> postForm(String url, Map<String, String> params, Map<String, String> headers) {
        logger.debug("发送POST表单请求: {}", url);

        try {
            FormBody.Builder formBuilder = new FormBody.Builder();

            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
            }

            RequestBody body = formBuilder.build();

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.debug("POST表单请求响应: code={}, body={}", response.code(), responseBody);

                if (response.isSuccessful()) {
                    return objectMapper.readValue(responseBody, Map.class);
                } else {
                    logger.warn("POST表单请求失败: code={}, message={}", response.code(), response.message());
                    return createErrorResponse(response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("POST表单请求异常: url={}, error={}", url, e.getMessage(), e);
            return createErrorResponse(500, "网络请求异常: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     *
     * @param url 上传URL
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @param params 额外参数
     * @return 响应结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadFile(String url, byte[] fileBytes, String fileName, Map<String, String> params) {
        logger.debug("上传文件: url={}, fileName={}, size={}", url, fileName, fileBytes.length);

        try {
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // 添加文件
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes);
            multipartBuilder.addFormDataPart("file", fileName, fileBody);

            // 添加额外参数
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }

            RequestBody body = multipartBuilder.build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                logger.debug("文件上传响应: code={}, body={}", response.code(), responseBody);

                if (response.isSuccessful()) {
                    return objectMapper.readValue(responseBody, Map.class);
                } else {
                    logger.warn("文件上传失败: code={}, message={}", response.code(), response.message());
                    return createErrorResponse(response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("文件上传异常: url={}, fileName={}, error={}", url, fileName, e.getMessage(), e);
            return createErrorResponse(500, "文件上传异常: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 错误响应
     */
    private Map<String, Object> createErrorResponse(int code, String message) {
        Map<String, Object> errorResponse = new java.util.HashMap<>();
        errorResponse.put("code", String.valueOf(code));
        errorResponse.put("message", message);
        errorResponse.put("success", false);
        return errorResponse;
    }

    /**
     * 关闭HTTP客户端
     */
    public void close() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            logger.info("HTTP客户端已关闭");
        }
    }
}
