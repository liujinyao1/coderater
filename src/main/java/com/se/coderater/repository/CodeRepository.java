package com.se.coderater.repository;

import com.se.coderater.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CodeRepository extends JpaRepository<Code, Long> {
    // 根据上传者ID查找代码列表
    List<Code> findByUploaderId(Long userId); // 我们之前可能已经有这个了
    List<Code> findByUploaderUsername(String username); // 新增：根据用户名查找更方便
}