package com.se.coderater.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCodeRequest {

    @NotBlank(message = "File name cannot be blank")
    @Size(max = 255, message = "File name cannot exceed 255 characters")
    // 可以在 Service 层进一步校验是否以 .java 结尾
    private String fileName;

    @NotBlank(message = "Code content cannot be blank")
    private String content;
}