package com.se.coderater.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy; // 如果后续使用JWT
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 启用 Spring Security 的 Web 安全支持
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF (Cross-Site Request Forgery)
                // 对于无状态的 RESTful API (特别是使用 Token 认证时)，CSRF 通常是不必要的
                .csrf(csrf -> csrf.disable())

                // 2. 配置请求授权规则
                .authorizeHttpRequests(authz -> authz
                        // 允许对 /api/auth/** (注册和登录) 的所有请求，无需认证
                        .requestMatchers("/api/auth/**").permitAll()
                        // 允许对 POST /api/code/upload 的请求，无需认证 (为了我们当前的测试)
                        .requestMatchers(HttpMethod.POST, "/api/code/upload").permitAll()
                        // 允许对 GET /api/analysis/** 的请求，无需认证 (如果后续分析接口也需要先测试)
                        // .requestMatchers(HttpMethod.GET, "/api/analysis/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                );

        // 3. 配置会话管理 (如果后续使用JWT，可以设置为无状态)
        // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 4. 如果需要，可以配置 HTTP Basic 认证或表单登录 (Spring Security 默认会提供)
        // .httpBasic(Customizer.withDefaults()); // 启用 HTTP Basic
        // .formLogin(Customizer.withDefaults()); // 启用表单登录

        return http.build();
    }

    // 如果我们暂时不使用密码加密，可以不配置 PasswordEncoder Bean
    // 但在实现用户注册时，这是必需的
    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }
}