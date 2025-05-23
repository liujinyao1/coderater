package com.se.coderater.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSummaryDTO {
    private Long id;
    private String fileName;
    private String uploaderUsername; // 从 Code.uploader.username 获取
    private LocalDateTime uploadedAt;
    private Integer lineCount;
    // 你可以根据需要添加 classCount, methodCount 等摘要信息
}