package com.mls.upload.client.service;

import com.mls.upload.client.model.dto.LoginRequest;
import com.mls.upload.client.util.ConfigUtil;
import com.mls.upload.client.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 * 处理用户登录和身份验证
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private HttpUtil httpUtil;
    private String serverUrl;
    private String currentUsername;
    private String currentToken;

    public AuthService() {
        this.httpUtil = new HttpUtil();
        this.serverUrl = ConfigUtil.getServerUrl();
        logger.info("认证服务初始化完成，服务器地址: {}", serverUrl);
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录是否成功
     */
    public boolean login(String username, String password) {
        logger.info("开始用户登录: username={}", username);

        try {
            // 构建登录请求
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(username);
            loginRequest.setPassword(password);

            // 发送登录请求
            String loginUrl = serverUrl + "/api/auth/login";
            Map<String, Object> response = httpUtil.postJson(loginUrl, loginRequest);

            if (response != null && "200".equals(String.valueOf(response.get("code")))) {
                // 登录成功
                this.currentUsername = username;

                // 提取token（如果有）
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("token")) {
                    this.currentToken = (String) data.get("token");
                }

                logger.info("用户登录成功: username={}", username);
                return true;
            } else {
                String message = response != null ? (String) response.get("message") : "未知错误";
                logger.warn("用户登录失败: username={}, message={}", username, message);
                return false;
            }

        } catch (Exception e) {
            logger.error("用户登录异常: username={}, error={}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查用户是否已登录
     *
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isEmpty();
    }

    /**
     * 获取当前登录用户名
     *
     * @return 当前用户名
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * 获取当前用户token
     *
     * @return 当前token
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * 用户登出
     */
    public void logout() {
        logger.info("用户登出: username={}", currentUsername);
        this.currentUsername = null;
        this.currentToken = null;
    }

    /**
     * 测试服务器连接
     *
     * @return 连接是否成功
     */
    public boolean testConnection() {
        logger.info("测试服务器连接: {}", serverUrl);

        try {
            String healthUrl = serverUrl + "/api/auth/health";
            Map<String, Object> response = httpUtil.get(healthUrl);

            boolean connected = response != null && "200".equals(String.valueOf(response.get("code")));
            logger.info("服务器连接测试结果: {}", connected ? "成功" : "失败");
            return connected;

        } catch (Exception e) {
            logger.error("服务器连接测试异常: {}", e.getMessage(), e);
            return false;
        }
    }
}
