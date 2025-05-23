package com.se.coderater.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference; // 用于处理序列化时的循环引用

@Entity
@Table(name = "codes") // 表名 codes
@Data // Lombok: 自动生成 getter, setter, toString, equals, hashCode
@NoArgsConstructor // Lombok: 自动生成无参构造函数
@AllArgsConstructor // Lombok: 自动生成全参构造函数
public class Code {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增
    private Long id;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User uploader;

    @Column(nullable = false)
    private String fileName; // 文件名

    @Lob // 表示这是一个大对象，适合存储较长文本
    @Column(nullable = false, columnDefinition = "TEXT") // 明确指定数据库类型为TEXT
    private String content; // 代码内容

    private LocalDateTime uploadedAt; // 上传时间

    // 解析结果字段
    private Integer classCount;    // 类数量
    private Integer methodCount;   // 方法数量
    private Integer lineCount;     // 代码行数 (不含空行和注释的有效行数，或总行数，根据需求定)


    @OneToOne(mappedBy = "code", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Analysis analysis;

    @PrePersist // JPA 回调方法，在实体持久化之前执行
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    // 为了方便测试，我们可以添加一个构造函数，不包含 uploader 和 analysis
    public Code(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }
}