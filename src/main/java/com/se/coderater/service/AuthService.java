package com.se.coderater.service;

import com.se.coderater.dto.LoginRequest;
import com.se.coderater.dto.RegisterRequest;
import com.se.coderater.dto.AuthResponse;
import com.se.coderater.entity.User;
import com.se.coderater.repository.UserRepository;
import com.se.coderater.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager authenticationManager; // 用于用户认证

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder; // 用于密码加密

    @Autowired
    JwtUtils jwtUtils; // 用于生成JWT

    public AuthResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            // 返回错误信息，或者抛出自定义异常由全局异常处理器处理
            return new AuthResponse("Error: Username is already taken!", null);
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new AuthResponse("Error: Email is already in use!", null);
        }

        // 创建新用户
        User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                encoder.encode(registerRequest.getPassword()) // 加密密码
        );

        // 设置默认角色 (例如 ROLE_USER)
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER"); // 默认给新用户分配 ROLE_USER 角色
        user.setRoles(roles);

        userRepository.save(user);

        // 注册成功后，可以选择直接登录并返回JWT，或者仅提示注册成功
        // 这里我们仅提示注册成功
        return new AuthResponse("User registered successfully!", null);
    }

    public AuthResponse loginUser(LoginRequest loginRequest) {
        // 使用 AuthenticationManager 进行用户认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 如果认证成功，将 Authentication 对象设置到 SecurityContext 中
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成 JWT
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 从 Authentication 对象中获取 UserDetails (这里是我们的 User 对象)
        User userDetails = (User) authentication.getPrincipal();

        // 返回JWT和一些用户信息 (可以根据需要定制 AuthResponse DTO)
        return new AuthResponse(
                "User logged in successfully!",
                jwt
                // 可以添加用户角色等信息到 AuthResponse DTO 中
                // userDetails.getRoles().stream().findFirst().orElse(null) // 示例：获取第一个角色
        );
    }
}