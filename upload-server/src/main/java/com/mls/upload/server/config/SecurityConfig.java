package com.mls.upload.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security配置
 * 
 * @author MLS Development Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护（对于API服务）
            .csrf().disable()
            
            // 配置请求授权
            .authorizeRequests()
                // 允许健康检查端点无需认证
                .antMatchers("/auth/health").permitAll()
                .antMatchers("/upload/health").permitAll()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                // 允许登录端点无需认证
                .antMatchers("/auth/login").permitAll()
                // 临时允许上传接口无需认证（用于测试）
                .antMatchers("/upload/**").permitAll()
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            
            // 禁用表单登录（使用API方式）
            .and()
            .formLogin().disable()
            
            // 禁用HTTP Basic认证
            .httpBasic().disable()
            
            // 配置会话管理为无状态（适合API）
            .sessionManagement()
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS);
    }

    /**
     * 密码编码器Bean
     * 
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
