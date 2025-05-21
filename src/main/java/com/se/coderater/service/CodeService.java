package com.se.coderater.service;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.se.coderater.entity.Code;
import com.se.coderater.repository.CodeRepository;
import org.slf4j.Logger; // 用于日志记录
import org.slf4j.LoggerFactory; // 用于日志记录
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class CodeService {

    private static final Logger logger = LoggerFactory.getLogger(CodeService.class); // 日志记录器
    private final CodeRepository codeRepository;

    @Autowired
    public CodeService(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    public Code storeFileAndParse(MultipartFile file) throws IOException, IllegalArgumentException {
        // 1. 校验文件名和文件类型 (与之前 storeFile 方法一致)
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must have a name.");
        }
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (!"java".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("Only .java files are allowed.");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // 2. 读取文件内容
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // 3. 创建 Code 实体
        Code newCode = new Code();
        newCode.setFileName(originalFileName);
        newCode.setContent(content);

        // 4. 使用 JavaParser 解析代码
        try {
            CompilationUnit cu = StaticJavaParser.parse(content);

            // 统计类数量 (包括接口、枚举、注解类型)
            // findAll(ClassOrInterfaceDeclaration.class) 会找到所有的类和接口声明
            // 你可以根据需要更精确地过滤，例如只统计 public class
            int classCount = cu.findAll(ClassOrInterfaceDeclaration.class).size();
            newCode.setClassCount(classCount);

            // 统计方法数量
            int methodCount = cu.findAll(MethodDeclaration.class).size();
            newCode.setMethodCount(methodCount);

            // 统计代码行数 (这里我们统计非空行数作为示例)
            // 你也可以直接使用 content.lines().count() 来获取总行数
            long nonEmptyLines = content.lines().filter(line -> !line.trim().isEmpty()).count();
            newCode.setLineCount((int) nonEmptyLines); // 注意类型转换

            logger.info("Parsed {}: Classes={}, Methods={}, Lines={}", originalFileName, classCount, methodCount, nonEmptyLines);

        } catch (ParseProblemException e) {
            // 如果 Java 代码有语法错误，JavaParser 会抛出 ParseProblemException
            logger.error("Failed to parse Java file: {}. Reason: {}", originalFileName, e.getMessage());
            // 你可以选择如何处理：
            // 1. 仍然保存文件，但解析相关的字段为 null 或 0 (当前实现会是这样，因为字段是Integer，默认为null)
            // 2. 抛出异常，不允许保存语法错误的文件
            // 3. 保存文件，并记录错误信息到 Code 实体的一个新字段中
            // 这里我们选择记录日志，并继续保存（解析字段将保持null或默认值）
            // 如果希望严格一点，可以再次抛出异常：
            // throw new IllegalArgumentException("Invalid Java syntax in file: " + originalFileName + ". " + e.getMessage());
        } catch (Exception e) {
            // 其他可能的解析时异常
            logger.error("An unexpected error occurred during parsing file: {}. Reason: {}", originalFileName, e.getMessage());
        }


        // 5. 保存到数据库
        return codeRepository.save(newCode);
    }
}