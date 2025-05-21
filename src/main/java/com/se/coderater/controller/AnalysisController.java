package com.se.coderater.controller;

import com.se.coderater.entity.Analysis;
import com.se.coderater.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Autowired
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // 通常分析是针对已上传的代码，所以用POST请求，并传入codeId作为路径变量
    @PostMapping("/{codeId}")
    public ResponseEntity<?> performAnalysis(@PathVariable Long codeId) {
        try {
            Analysis analysisResult = analysisService.analyzeCode(codeId);
            return ResponseEntity.ok(analysisResult);
        } catch (IllegalArgumentException e) { // 例如 codeId 不存在
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) { // 包括 IOException, CheckstyleException 等
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Analysis Failed");
            errorResponse.put("message", "An error occurred during code analysis: " + e.getMessage());
            // 打印堆栈信息到服务器日志，方便调试
            e.printStackTrace(); // 或者使用 logger.error("Analysis failed for codeId: {}", codeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 可选：获取特定代码的分析结果 (如果需要单独查询)
    // @GetMapping("/{codeId}")
    // public ResponseEntity<?> getAnalysisResult(@PathVariable Long codeId) {
    //     // 实现从 analysisRepository 查询并返回的逻辑
    //     // 注意处理 Analysis 可能不存在的情况
    // }
}