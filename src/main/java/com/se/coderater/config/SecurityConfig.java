package com.se.coderater.config;

import com.se.coderater.security.AuthEntryPointJwt;
import com.se.coderater.security.AuthTokenFilter;
import com.se.coderater.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // 用于方法级别的安全注解
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 可选: 如果你需要在方法上使用 @PreAuthorize 等注解
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(); // 我们需要将 AuthTokenFilter 声明为一个 Bean
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // 设置 UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder());     // 设置 PasswordEncoder
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager(); // 从 AuthenticationConfiguration 获取 AuthenticationManager
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用 BCrypt 进行密码加密
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // 禁用 CSRF
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // 配置未授权处理器
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 设置会话管理为无状态 (因为我们用JWT)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll() // 允许对 /api/auth/** (注册和登录) 的所有请求
                        .requestMatchers(HttpMethod.GET, "/api/code/public/list").permitAll() // 新增：公开的代码列表
                        .requestMatchers(HttpMethod.POST, "/api/code/upload").authenticated() // 允许上传 (后续可以改为需要认证)
                        .requestMatchers(HttpMethod.GET, "/api/code/mycode").authenticated() // 新增：获取自己的代码列表
                        .requestMatchers(HttpMethod.GET, "/api/code/{codeId}").authenticated() // 新增：获取自己的代码详情
                        .requestMatchers(HttpMethod.PUT, "/api/code/{codeId}/filename").authenticated() // 新增：修改文件名
                        .requestMatchers(HttpMethod.DELETE, "/api/code/{codeId}").authenticated()   // 新增：删除代码
                        .requestMatchers(HttpMethod.POST, "/api/analysis/**").authenticated() // 分析也需要认证 (Service层做所有权校验)
                        .requestMatchers("/api/user/me").authenticated()
                        // TODO: 对于其他接口，例如获取代码列表、获取用户信息等，需要配置为 .authenticated()
                        .anyRequest().authenticated() // 其他所有请求都需要认证
                );

        // 添加 DaoAuthenticationProvider
        http.authenticationProvider(authenticationProvider());
        // 在 UsernamePasswordAuthenticationFilter 之前添加我们自定义的 JWT 过滤器
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}