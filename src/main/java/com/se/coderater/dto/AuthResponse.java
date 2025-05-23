package com.se.coderater.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String accessToken; // 用于存放 JWT
    // 你可以根据需要添加其他字段，例如：
    // private String tokenType = "Bearer";
    // private Long userId;
    // private String username;
    // private List<String> roles;

    // 为了方便 AuthService 中只返回消息或只返回错误的情况
    public AuthResponse(String message) {
        this.message = message;
    }
}