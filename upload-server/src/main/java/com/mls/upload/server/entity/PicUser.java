package com.mls.upload.server.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库pic_user表
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
public class PicUser {

    /**
     * 用户ID（主键，自增）
     */
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密后）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updataTime;

    // 默认构造函数
    public PicUser() {
    }

    // 带参构造函数
    public PicUser(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.createTime = LocalDateTime.now();
        this.updataTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdataTime() {
        return updataTime;
    }

    public void setUpdataTime(LocalDateTime updataTime) {
        this.updataTime = updataTime;
    }

    @Override
    public String toString() {
        return "PicUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", createTime=" + createTime +
                ", updataTime=" + updataTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PicUser picUser = (PicUser) o;

        return id != null ? id.equals(picUser.id) : picUser.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
