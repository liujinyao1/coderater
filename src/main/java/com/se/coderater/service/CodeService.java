package com.se.coderater.service;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.se.coderater.entity.Analysis;
import com.se.coderater.repository.AnalysisRepository;
import com.se.coderater.entity.Code;
import com.se.coderater.repository.CodeRepository;
import org.slf4j.Logger; // 用于日志记录
import org.slf4j.LoggerFactory; // 用于日志记录
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import com.se.coderater.entity.User; // 确保导入
import com.se.coderater.repository.UserRepository; // 需要注入 UserRepository
import org.springframework.security.core.Authentication; // 用于获取认证信息
import org.springframework.security.core.context.SecurityContextHolder; // 用于获取当前安全上下文
import org.springframework.security.core.userdetails.UsernameNotFoundException; // 用于用户未找到异常
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException; // 用于权限不足的异常
import com.se.coderater.dto.CodeSummaryDTO; // 导入 DTO
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
public class CodeService {

    private static final Logger logger = LoggerFactory.getLogger(CodeService.class); // 日志记录器
    private final UserRepository userRepository; // 新增注入
    private final CodeRepository codeRepository;

    @Autowired
    public CodeService(CodeRepository codeRepository, UserRepository userRepository) { // 修改构造函数
        this.codeRepository = codeRepository;
        this.userRepository = userRepository; // 初始化
    }



    public Code storeFileAndParse(MultipartFile file) throws IOException, IllegalArgumentException {
        // 1. 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果没有认证用户，或者用户是匿名用户，则不允许上传
            // 注意：如果接口本身是 permitAll()，这里可能需要调整逻辑或依赖于接口层面的认证检查
            throw new IllegalStateException("User must be authenticated to upload code.");
        }
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername + ". Cannot upload code."));
        // 2. 校验文件名和文件类型 (与之前 storeFile 方法一致)
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

        // 3. 读取文件内容
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // 4. 创建 Code 实体
        Code newCode = new Code();
        newCode.setFileName(originalFileName);
        newCode.setContent(content);
        newCode.setUploader(currentUser); // 关联当前登录用户

        // 5. 使用 JavaParser 解析代码
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


        // 6. 保存到数据库
        return codeRepository.save(newCode);
    }
    // CodeService.java
// ...
    public List<Code> getCodesForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User must be authenticated to view their codes.");
        }
        String currentUsername = authentication.getName();
        // 使用 findByUploaderUsername 可以避免再次查询 User 对象
        return codeRepository.findByUploaderUsername(currentUsername);
    }

    // 可选：根据 codeId 获取代码详情，并检查是否属于当前用户
    public Optional<Code> getCodeByIdForCurrentUser(Long codeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User must be authenticated to view this code."); // 或者返回 Optional.empty()
        }
        String currentUsername = authentication.getName();

        Optional<Code> codeOpt = codeRepository.findById(codeId);
        if (codeOpt.isPresent()) {
            if (codeOpt.get().getUploader().getUsername().equals(currentUsername)) {
                return codeOpt; // 用户是所有者，返回代码
            } else {
                // 用户不是所有者，抛出 AccessDeniedException
                throw new AccessDeniedException("You do not have permission to view this code.");
            }
        }
        return Optional.empty(); // 代码本身未找到
    }
    @Transactional // 确保数据库操作的原子性
    public void deleteCodeForCurrentUser(Long codeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User must be authenticated to delete code.");
        }
        String currentUsername = authentication.getName();

        Code codeToDelete = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));


        // 权限校验：确保代码属于当前登录用户
        if (!codeToDelete.getUploader().getUsername().equals(currentUsername)) {
            logger.warn("User '{}' attempted to delete code '{}' owned by '{}'. Access denied.",
                    currentUsername, codeId, codeToDelete.getUploader().getUsername());
            throw new AccessDeniedException("You do not have permission to delete this code.");
        }

        // 如果有关联的 Analysis 记录，也需要考虑如何处理。
        // 1. 级联删除：如果 Code 和 Analysis 之间设置了 CascadeType.REMOVE 或 ALL，JPA 会自动删除。
        // 2. 手动删除：如果 Analysis 与 Code 的关联不是级联删除，且 Analysis 记录依赖 Code，
        //    你可能需要先手动删除 Analysis 记录，或者确保数据库外键设置了 ON DELETE CASCADE。
        //    假设我们已经在 Code 实体中对 Analysis 设置了级联删除 (e.g., @OneToOne(mappedBy = "code", cascade = CascadeType.ALL))
        //    或者 Analysis 实体中对 Code 的引用允许 Code 被删除。

        codeRepository.delete(codeToDelete);
        logger.info("User '{}' successfully deleted code with id: {}", currentUsername, codeId);
    }
    /*@Transactional
    public Code updateCodeFileNameForCurrentUser(Long codeId, String newFileName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User must be authenticated to update code.");
        }
        String currentUsername = authentication.getName();

        Code codeToUpdate = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));

        // 权限校验
        if (!codeToUpdate.getUploader().getUsername().equals(currentUsername)) {
            logger.warn("User '{}' attempted to update code '{}' owned by '{}'. Access denied.",
                    currentUsername, codeId, codeToUpdate.getUploader().getUsername());
            throw new AccessDeniedException("You do not have permission to update this code.");
        }

        // 校验新文件名 (例如，不能为空，必须以 .java 结尾)
        if (newFileName == null || newFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("New file name cannot be empty.");
        }
        if (!newFileName.trim().toLowerCase().endsWith(".java")) {
            throw new IllegalArgumentException("File name must end with .java");
        }

        codeToUpdate.setFileName(newFileName.trim());
        Code updatedCode = codeRepository.save(codeToUpdate);
        logger.info("User '{}' successfully updated file name for code id: {} to '{}'", currentUsername, codeId, newFileName);
        return updatedCode;
    }*/
    public Page<CodeSummaryDTO> getPublicCodeSummaries(Pageable pageable) {
        Page<Code> codePage = codeRepository.findAll(pageable); // 获取分页的 Code 实体

        // 将 Page<Code> 转换为 Page<CodeSummaryDTO>
        return codePage.map(code -> new CodeSummaryDTO(
                code.getId(),
                code.getFileName(),
                code.getUploader() != null ? code.getUploader().getUsername() : "Unknown", // 处理 uploader 可能为 null 的情况
                code.getUploadedAt(),
                code.getLineCount()
        ));
    }
    @Transactional
    public Code updateCodeDetailsForCurrentUser(Long codeId, String newFileName, String newContent) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("User must be authenticated to update code.");
        }
        String currentUsername = authentication.getName();

        Code codeToUpdate = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));

        // 权限校验
        if (!codeToUpdate.getUploader().getUsername().equals(currentUsername)) {
            logger.warn("User '{}' attempted to update code '{}' owned by '{}'. Access denied.",
                    currentUsername, codeId, codeToUpdate.getUploader().getUsername());
            throw new AccessDeniedException("You do not have permission to update this code.");
        }

        // 校验新文件名和内容
        if (newFileName == null || newFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("New file name cannot be empty.");
        }
        if (!newFileName.trim().toLowerCase().endsWith(".java")) {
            throw new IllegalArgumentException("File name must end with .java");
        }
        if (newContent == null || newContent.isEmpty()) {
            throw new IllegalArgumentException("Code content cannot be empty.");
        }

        // 更新文件名和内容
        codeToUpdate.setFileName(newFileName.trim());
        codeToUpdate.setContent(newContent);
        codeToUpdate.setUploadedAt(LocalDateTime.now()); // 手动更新时间

        // **重新解析新的代码内容，并更新 Code 实体的统计字段**
        try {
            CompilationUnit cu = StaticJavaParser.parse(newContent);
            codeToUpdate.setClassCount(cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).size());
            codeToUpdate.setMethodCount(cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).size());
            codeToUpdate.setLineCount((int) newContent.lines().filter(line -> !line.trim().isEmpty()).count());
            logger.info("Re-parsed content for codeId: {} after update. Counts: Class={}, Method={}, Line={}",
                    codeId, codeToUpdate.getClassCount(), codeToUpdate.getMethodCount(), codeToUpdate.getLineCount());
        } catch (Exception e) {
            logger.error("Failed to re-parse updated Java content for codeId: {}. Setting counts to null. Reason: {}", codeId, e.getMessage());
            // 如果解析失败，将统计数据设为 null 或 0，以表示当前内容无法解析
            codeToUpdate.setClassCount(null);
            codeToUpdate.setMethodCount(null);
            codeToUpdate.setLineCount(null);
        }


        Code updatedCode = codeRepository.save(codeToUpdate);
        logger.info("User '{}' successfully updated details (content and stats) for code id: {}", currentUsername, codeId);
        return updatedCode;
    }
// ...
}