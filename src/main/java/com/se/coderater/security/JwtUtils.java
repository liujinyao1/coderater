package com.se.coderater.security;

import com.se.coderater.entity.User; // 或者使用 UserDetails 主体
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // JSR-250
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}") // 从 application.properties 读取密钥
    private String jwtSecretString;

    @Value("${jwt.expiration.ms}") // 从 application.properties 读取过期时间
    private int jwtExpirationMs;

    private Key key; // 用于签名的密钥对象

    @PostConstruct // 在依赖注入完成后执行，用于初始化 key
    public void init() {
        // 将字符串密钥转换为 Key 对象
        // 对于 HS512 算法，密钥长度至少需要 64 字节 (512位)
        // 如果 jwtSecretString 不够长，Keys.hmacShaKeyFor 会抛异常或生成不安全的密钥
        // 确保你的 jwt.secret 足够长且安全
        if (jwtSecretString.getBytes().length < 64) {
            logger.warn("JWT secret is too short for HS512. Consider using a longer secret or a different algorithm.");
            // 可以选择抛出异常或使用一个默认的更安全的密钥生成方式，但这里我们先按配置来
        }
        this.key = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        // 从 Authentication 对象获取用户信息
        User userPrincipal = (User) authentication.getPrincipal(); // 假设 principal 是 User 对象

        // 获取用户的角色/权限
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // 主题，通常是用户名
                .claim("roles", authorities) // 自定义声明，存储角色
                .claim("userId", userPrincipal.getId()) // 存储用户ID
                .setIssuedAt(new Date()) // 发布时间
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS512) // 使用HS512算法和密钥签名
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }


    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}