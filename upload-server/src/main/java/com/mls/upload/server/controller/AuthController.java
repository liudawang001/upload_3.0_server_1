package com.mls.upload.server.controller;

import com.mls.upload.server.dto.ApiResponse;
import com.mls.upload.server.dto.LoginRequest;
import com.mls.upload.server.dto.LoginResponse;
import com.mls.upload.server.entity.PicUser;
import com.mls.upload.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器
 * 处理用户登录和身份验证相关的REST API
 *
 * @author MLS Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*") // 允许跨域请求
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("收到登录请求: username={}", loginRequest.getUsername());

        try {
            LoginResponse loginResponse = authService.login(loginRequest);

            if (loginResponse != null) {
                logger.info("用户登录成功: username={}", loginRequest.getUsername());
                return ApiResponse.success("登录成功", loginResponse);
            } else {
                logger.warn("用户登录失败: username={}", loginRequest.getUsername());
                return ApiResponse.unauthorized("用户名或密码错误");
            }

        } catch (Exception e) {
            logger.error("登录处理异常: username={}, error={}",
                        loginRequest.getUsername(), e.getMessage(), e);
            return ApiResponse.error("登录处理异常，请稍后重试");
        }
    }

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @return 注册响应
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam(required = false) String nickname) {
        logger.info("收到注册请求: username={}, nickname={}", username, nickname);

        try {
            boolean success = authService.register(username, password, nickname);

            if (success) {
                logger.info("用户注册成功: username={}", username);
                return ApiResponse.success("注册成功");
            } else {
                logger.warn("用户注册失败: username={}", username);
                return ApiResponse.error("注册失败，用户名可能已存在");
            }

        } catch (Exception e) {
            logger.error("注册处理异常: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.error("注册处理异常，请稍后重试");
        }
    }

    /**
     * 检查用户是否存在
     *
     * @param username 用户名
     * @return 检查结果
     */
    @GetMapping("/check-user")
    public ApiResponse<Boolean> checkUser(@RequestParam String username) {
        logger.info("检查用户存在性: username={}", username);

        try {
            boolean exists = authService.userExists(username);
            return ApiResponse.success("检查完成", exists);

        } catch (Exception e) {
            logger.error("检查用户存在性异常: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.error("检查用户存在性异常");
        }
    }

    /**
     * 获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/user-info")
    public ApiResponse<PicUser> getUserInfo(@RequestParam String username) {
        logger.info("获取用户信息: username={}", username);

        try {
            PicUser user = authService.getUserByUsername(username);

            if (user != null) {
                return ApiResponse.success("获取用户信息成功", user);
            } else {
                return ApiResponse.notFound("用户不存在");
            }

        } catch (Exception e) {
            logger.error("获取用户信息异常: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.error("获取用户信息异常");
        }
    }

    /**
     * 修改密码
     *
     * @param username 用户名
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestParam String username,
                                           @RequestParam String oldPassword,
                                           @RequestParam String newPassword) {
        logger.info("收到修改密码请求: username={}", username);

        try {
            boolean success = authService.changePassword(username, oldPassword, newPassword);

            if (success) {
                logger.info("密码修改成功: username={}", username);
                return ApiResponse.success("密码修改成功");
            } else {
                logger.warn("密码修改失败: username={}", username);
                return ApiResponse.error("密码修改失败，请检查旧密码是否正确");
            }

        } catch (Exception e) {
            logger.error("修改密码异常: username={}, error={}", username, e.getMessage(), e);
            return ApiResponse.error("修改密码异常，请稍后重试");
        }
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("认证服务运行正常", "OK");
    }
}
