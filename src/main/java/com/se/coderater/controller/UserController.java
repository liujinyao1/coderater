package com.se.coderater.controller;

import com.se.coderater.dto.UserProfileDTO; // 需要创建这个 DTO
import com.se.coderater.entity.User;
import com.se.coderater.service.UserService; // 需要创建这个 Service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService; // 假设我们创建一个 UserService

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserDetails() {
        try {
            UserProfileDTO userProfile = userService.getCurrentUserProfile();
            return ResponseEntity.ok(userProfile);
        } catch (UsernameNotFoundException e) { // 或者 IllegalStateException
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "User not found or not authenticated");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse); // 401 或 404
        }
    }
}