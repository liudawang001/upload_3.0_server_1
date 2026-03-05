package com.mls.upload.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图片上传服务端主启动类
 * 基于Spring Boot 2.1.18的REST API服务
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.mls.upload.server.mapper")
@EnableConfigurationProperties
public class UploadServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadServerApplication.class);
    
    /**
     * 主方法 - 程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            logger.info("正在启动木林森图片上传服务端...");
            
            // 启动Spring Boot应用
            SpringApplication.run(UploadServerApplication.class, args);
            
            logger.info("服务端启动成功");
            
        } catch (Exception e) {
            logger.error("服务端启动失败", e);
            System.exit(1);
        }
    }
}
