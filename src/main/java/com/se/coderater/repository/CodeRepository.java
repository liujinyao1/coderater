package com.se.coderater.repository;


import com.se.coderater.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 可选，但推荐加上

import java.util.List; // 如果需要根据其他条件查询，例如文件名

@Repository // 明确这是一个 Spring 管理的 Repository Bean
public interface CodeRepository extends JpaRepository<Code, Long> {

    // JpaRepository 已经提供了 CRUD 方法，例如：
    // save(Code entity)
    // findById(Long id)
    // findAll()
    // deleteById(Long id)
    // ...等

    // 我们可以根据需要添加自定义的查询方法
    // 例如，根据文件名查找 (虽然在这个项目中可能用处不大，仅作示例)
    // List<Code> findByFileName(String fileName);

    // 如果后续添加了 User 关联，可以有类似这样的方法：
    // List<Code> findByUploaderId(Long userId);
}