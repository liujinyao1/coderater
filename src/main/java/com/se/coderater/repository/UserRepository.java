package com.se.coderater.repository;

import com.se.coderater.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查找用户 (Spring Security UserDetailsService 会用到)
    Optional<User> findByUsername(String username);

    // 检查用户名是否存在 (用于注册时校验)
    Boolean existsByUsername(String username);

    // 检查邮箱是否存在 (用于注册时校验)
    Boolean existsByEmail(String email);
}