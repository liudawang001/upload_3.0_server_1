package com.mls.upload.client.model.dto;

/**
 * 登录请求数据传输对象
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class LoginRequest {

    private String username;    // 用户名
    private String password;    // 密码
    private String clientType;  // 客户端类型
    private String version;     // 客户端版本

    public LoginRequest() {
        this.clientType = "DESKTOP";
        this.version = "1.0.0";
    }

    public LoginRequest(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // Getter和Setter方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", clientType='" + clientType + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
