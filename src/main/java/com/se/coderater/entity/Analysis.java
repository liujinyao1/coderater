package com.se.coderater.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonBackReference; // 导入
// import java.util.List; // 如果要存储详细的Checkstyle问题列表

@Entity
@Table(name = "analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id", nullable = false, unique = true)
    @JsonBackReference("code-analysis") // 使用与 Code 中 @JsonManagedReference 相同的名字
    private Code code;


    // Checkstyle 相关
    private Integer styleIssueCount; // Checkstyle 发现的问题总数

    // @Lob
    // @Column(columnDefinition = "TEXT")
    // private String styleIssuesDetails; // 可选：存储详细的Checkstyle问题报告 (JSON格式字符串)

    // 复杂度相关
    private Integer cyclomaticComplexity; // 平均或最大圈复杂度

    // 可读性相关
    private Double commentRatio; // 注释行数 / (非空代码行数) 的比例
    private Integer commentLineCount; // 注释行数量
    private Integer nonEmptyLineCount; // 非空行数量 (可以从 Code 实体获取或重新计算)

    private LocalDateTime analyzedAt;
    @Transient // 表示这个字段不映射到数据库表列
    private Integer overallScore; // 综合评分 (0-100)

    // 你也可以把每个子项的得分也作为瞬时字段放进来，方便前端展示
    @Transient
    private Integer styleScore;
    @Transient
    private Integer complexityScore;
    @Transient
    private Integer commentScore;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}