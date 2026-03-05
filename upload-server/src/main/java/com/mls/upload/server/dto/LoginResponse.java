package com.mls.upload.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 登录响应数据传输对象
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
public class LoginResponse {
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 访问令牌（如果需要的话）
     */
    private String token;
    
    /**
     * 令牌过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tokenExpireTime;
    
    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
    
    /**
     * 服务器信息
     */
    private String serverInfo;
    
    /**
     * 用户权限列表（预留）
     */
    private String[] permissions;
    
    // 默认构造函数
    public LoginResponse() {
        this.loginTime = LocalDateTime.now();
    }
    
    // 带参构造函数
    public LoginResponse(Integer userId, String username, String nickname) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.loginTime = LocalDateTime.now();
    }
    
    // 静态工厂方法
    public static LoginResponse success(Integer userId, String username, String nickname) {
        return new LoginResponse(userId, username, nickname);
    }
    
    public static LoginResponse success(Integer userId, String username, String nickname, String token) {
        LoginResponse response = new LoginResponse(userId, username, nickname);
        response.setToken(token);
        // 设置令牌过期时间为24小时后
        response.setTokenExpireTime(LocalDateTime.now().plusHours(24));
        return response;
    }
    
    // Getter和Setter方法
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public LocalDateTime getTokenExpireTime() {
        return tokenExpireTime;
    }
    
    public void setTokenExpireTime(LocalDateTime tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public String getServerInfo() {
        return serverInfo;
    }
    
    public void setServerInfo(String serverInfo) {
        this.serverInfo = serverInfo;
    }
    
    public String[] getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }
    
    /**
     * 判断令牌是否过期
     * 
     * @return true表示过期，false表示未过期
     */
    public boolean isTokenExpired() {
        if (tokenExpireTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(tokenExpireTime);
    }
    
    @Override
    public String toString() {
        return "LoginResponse{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", loginTime=" + loginTime +
                ", tokenExpireTime=" + tokenExpireTime +
                ", serverInfo='" + serverInfo + '\'' +
                ", hasToken=" + (token != null && !token.isEmpty()) +
                '}';
    }
}
