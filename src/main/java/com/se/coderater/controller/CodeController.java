package com.se.coderater.controller;

import com.se.coderater.entity.Code;
import com.se.coderater.service.CodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.constraints.NotBlank; // 用于请求参数校验
import org.springframework.data.domain.Page; // 导入 Page
import org.springframework.data.domain.Pageable; // 导入 Pageable
import org.springframework.data.web.PageableDefault; // 可选，用于设置默认分页参数
import com.se.coderater.dto.CodeSummaryDTO; // 导入 DTO
import com.se.coderater.dto.UpdateCodeRequest; // 导入新的 DTO
import jakarta.validation.Valid; // 用于校验请求体
@RestController
@RequestMapping("/api/code")
public class CodeController {

    private final CodeService codeService;

    @Autowired
    public CodeController(CodeService codeService) {
        this.codeService = codeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCodeFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "File is empty or not provided.");
            errorResponse.put("message", "Please select a file to upload.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // 调用新的包含解析逻辑的方法
            Code savedCode = codeService.storeFileAndParse(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCode);
        } catch (IllegalStateException e) { // 例如用户未认证
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // 返回 401
        }catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid file or content."); // 错误信息可以更通用
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "File processing error.");
            errorResponse.put("message", "Could not read or store the file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred.");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/mycode")
    public ResponseEntity<?> getCurrentUserCodes() {
        try {
            List<Code> codes = codeService.getCodesForCurrentUser();
            if (codes.isEmpty()) {
                return ResponseEntity.ok("You have not uploaded any code yet.");
            }
            return ResponseEntity.ok(codes);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @GetMapping("/{codeId}")
    public ResponseEntity<?> getCodeDetails(@PathVariable Long codeId) {
        try {
            Optional<Code> codeOpt = codeService.getCodeByIdForCurrentUser(codeId);
            if (codeOpt.isPresent()) {
                return ResponseEntity.ok(codeOpt.get());
            } else {
                // 如果代码不存在，或者不属于当前用户，都返回 404 Not Found
                // 更细致的可以是 403 Forbidden 如果代码存在但不属于该用户
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Not Found");
                errorResponse.put("message", "Code not found or you do not have permission to view it.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (IllegalStateException e) { // 用户未认证
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }catch (AccessDeniedException e) { // 明确处理权限不足
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
    }
    /*@PutMapping("/{codeId}/filename") // 使用 PUT 请求更新资源
    public ResponseEntity<?> updateCodeFileName(
            @PathVariable Long codeId,
            @RequestParam @NotBlank String newFileName) { // 直接用 @RequestParam 获取新文件名
        try {
            Code updatedCode = codeService.updateCodeFileNameForCurrentUser(codeId, newFileName);
            return ResponseEntity.ok(updatedCode);
        } catch (IllegalArgumentException e) { // 例如 codeId 不存在或文件名无效
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (AccessDeniedException e) { // 权限不足
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalStateException e) { // 用户未认证
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }*/


    @DeleteMapping("/{codeId}")
    public ResponseEntity<?> deleteCode(@PathVariable Long codeId) {
        try {
            codeService.deleteCodeForCurrentUser(codeId);
            return ResponseEntity.ok(Map.of("message", "Code with id " + codeId + " deleted successfully."));
            // 或者返回 ResponseEntity.noContent().build(); (204 No Content)
        } catch (IllegalArgumentException e) { // 例如 codeId 不存在
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found"); // 或者 Bad Request
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (AccessDeniedException e) { // 权限不足
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalStateException e) { // 用户未认证
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    // API: 获取所有已上传代码的摘要分页列表
    @GetMapping("/public/list") // 使用一个更明确的路径，例如 /public/list 或直接 /codes
    public ResponseEntity<Page<CodeSummaryDTO>> getAllPublicCodeSummaries(
            @PageableDefault(size = 10, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // @PageableDefault 可以设置默认的分页大小和排序规则
        // 例如：每页10条，按上传时间倒序排列
        Page<CodeSummaryDTO> codeSummaries = codeService.getPublicCodeSummaries(pageable);
        return ResponseEntity.ok(codeSummaries);
    }
    @PutMapping("/{codeId}") // 使用 PUT 请求更新整个代码资源（文件名和内容）
    public ResponseEntity<?> updateCodeDetails(
            @PathVariable Long codeId,
            @Valid @RequestBody UpdateCodeRequest updateCodeRequest) { // 接收 DTO 并校验
        try {
            Code updatedCode = codeService.updateCodeDetailsForCurrentUser(
                    codeId,
                    updateCodeRequest.getFileName(),
                    updateCodeRequest.getContent()
            );
            return ResponseEntity.ok(updatedCode);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (AccessDeniedException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication Required");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        // 注意：如果 UpdateCodeRequest 的 @Valid 校验失败，
        // GlobalExceptionHandler 会处理 MethodArgumentNotValidException 并返回 400
    }
// ...
}