package com.se.coderater.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet; // 用于存储角色
import java.util.Set;    // 用于存储角色
import java.util.stream.Collectors;

@Entity
@Table(name = "users",
        uniqueConstraints = { // 添加唯一约束，确保用户名和邮箱的唯一性
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })
@Data
@NoArgsConstructor
public class User implements UserDetails { // 实现 UserDetails 接口

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(max = 120) // 存储加密后的密码，长度可能较长
    @Column(nullable = false)
    private String password;

    // 角色管理 (简单起见，我们用字符串集合存储角色名，例如 "ROLE_USER", "ROLE_ADMIN")
    // @ElementCollection 会创建一个独立的表来存储角色
    // FetchType.EAGER 因为 UserDetails 需要立即加载权限
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    // UserDetails 接口方法实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将角色字符串转换为 GrantedAuthority 对象
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 或者根据业务逻辑添加字段判断
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 或者根据业务逻辑添加字段判断
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 或者根据业务逻辑添加字段判断
    }

    @Override
    public boolean isEnabled() {
        return true; // 或者根据业务逻辑添加字段判断 (例如邮箱验证后才启用)
    }

    // 一个用户可以上传多个代码文件
    // mappedBy = "uploader": 表示这个关系由 Code 实体的 "uploader" 字段维护
    // cascade = CascadeType.ALL: 当删除用户时，也删除其所有代码 (根据需求决定，也可以是 CascadeType.PERSIST, MERGE 等，或者不级联删除)
    // fetch = FetchType.LAZY: 默认，当加载 User 时，不立即加载其 codes 集合，只有在实际访问 codes 时才加载
    @OneToMany(mappedBy = "uploader", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Code> codes = new HashSet<>();
    // 构造函数 (方便创建用户)
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}