package com.mls.upload.server.service;

import com.mls.upload.server.dto.LoginRequest;
import com.mls.upload.server.dto.LoginResponse;
import com.mls.upload.server.entity.PicUser;
import com.mls.upload.server.mapper.PicUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证服务
 * 处理用户认证和授权业务逻辑
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private PicUserMapper picUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录验证
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("用户登录请求: username={}", loginRequest.getUsername());

        try {
            // 参数验证
            if (!StringUtils.hasText(loginRequest.getUsername()) ||
                !StringUtils.hasText(loginRequest.getPassword())) {
                logger.warn("登录失败: 用户名或密码为空");
                return null;
            }

            // 查询用户
            PicUser user = picUserMapper.findByUsername(loginRequest.getUsername());
            if (user == null) {
                logger.warn("登录失败: 用户不存在, username={}", loginRequest.getUsername());
                return null;
            }

            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                logger.warn("登录失败: 密码错误, username={}", loginRequest.getUsername());
                return null;
            }

            // 登录成功，创建响应
            LoginResponse response = LoginResponse.success(
                user.getId(),
                user.getUsername(),
                user.getNickname()
            );

            // 设置服务器信息
            response.setServerInfo("MLS图片上传服务器 v1.0.0");

            logger.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

            return response;

        } catch (Exception e) {
            logger.error("用户登录异常: username={}, error={}",
                        loginRequest.getUsername(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @param nickname 昵称
     * @return 注册是否成功
     */
    public boolean register(String username, String password, String nickname) {
        logger.info("用户注册请求: username={}, nickname={}", username, nickname);

        try {
            // 参数验证
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                logger.warn("注册失败: 用户名或密码为空");
                return false;
            }

            // 检查用户名是否已存在
            if (picUserMapper.countByUsername(username) > 0) {
                logger.warn("注册失败: 用户名已存在, username={}", username);
                return false;
            }

            // 创建新用户
            PicUser newUser = new PicUser();
            newUser.setUsername(username);
            newUser.setPassword(passwordEncoder.encode(password)); // 加密密码
            newUser.setNickname(StringUtils.hasText(nickname) ? nickname : username);
            newUser.setCreateTime(LocalDateTime.now());
            newUser.setUpdataTime(LocalDateTime.now());

            // 保存用户
            int result = picUserMapper.insert(newUser);

            if (result > 0) {
                logger.info("用户注册成功: userId={}, username={}", newUser.getId(), username);
                return true;
            } else {
                logger.warn("用户注册失败: 数据库插入失败, username={}", username);
                return false;
            }

        } catch (Exception e) {
            logger.error("用户注册异常: username={}, error={}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证用户是否存在
     *
     * @param username 用户名
     * @return 用户是否存在
     */
    public boolean userExists(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        try {
            return picUserMapper.countByUsername(username) > 0;
        } catch (Exception e) {
            logger.error("检查用户存在性异常: username={}, error={}", username, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息（不包含密码）
     */
    public PicUser getUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        try {
            PicUser user = picUserMapper.findByUsername(username);
            if (user != null) {
                // 清除密码信息，确保安全
                user.setPassword(null);
            }
            return user;
        } catch (Exception e) {
            logger.error("获取用户信息异常: username={}, error={}", username, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新用户密码
     *
     * @param username 用户名
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 更新是否成功
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        logger.info("用户修改密码请求: username={}", username);

        try {
            // 参数验证
            if (!StringUtils.hasText(username) ||
                !StringUtils.hasText(oldPassword) ||
                !StringUtils.hasText(newPassword)) {
                logger.warn("修改密码失败: 参数为空");
                return false;
            }

            // 查询用户
            PicUser user = picUserMapper.findByUsername(username);
            if (user == null) {
                logger.warn("修改密码失败: 用户不存在, username={}", username);
                return false;
            }

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                logger.warn("修改密码失败: 旧密码错误, username={}", username);
                return false;
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdataTime(LocalDateTime.now());

            int result = picUserMapper.update(user);

            if (result > 0) {
                logger.info("用户密码修改成功: username={}", username);
                return true;
            } else {
                logger.warn("用户密码修改失败: 数据库更新失败, username={}", username);
                return false;
            }

        } catch (Exception e) {
            logger.error("用户密码修改异常: username={}, error={}", username, e.getMessage(), e);
            return false;
        }
    }
}
