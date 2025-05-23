package com.se.coderater.controller;

import com.se.coderater.dto.LoginRequest;
import com.se.coderater.dto.RegisterRequest;
import com.se.coderater.dto.AuthResponse;
import com.se.coderater.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600) // 可选: 允许跨域请求，方便前端调试
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.registerUser(registerRequest);
        if (response.getAccessToken() == null && response.getMessage().startsWith("Error:")) { // 检查是否有错误
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // 登录失败时，AuthService 中的 authenticationManager.authenticate 会抛出异常
        // 我们应该捕获这些异常并返回合适的HTTP状态码
        try {
            AuthResponse response = authService.loginUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) { // 例如 BadCredentialsException
            // 返回 401 Unauthorized 或其他合适的错误
            return ResponseEntity.status(401).body(new AuthResponse("Error: Invalid username or password", null));
        }
    }
}