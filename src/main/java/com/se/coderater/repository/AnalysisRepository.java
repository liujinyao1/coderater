package com.se.coderater.repository;

import com.se.coderater.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // 根据 Code ID 查找分析结果 (因为 code_id 是唯一的)
    Optional<Analysis> findByCodeId(Long codeId);

    // 检查是否存在针对某个 Code ID 的分析结果
    boolean existsByCodeId(Long codeId);
}