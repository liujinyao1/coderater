package com.se.coderater.service;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.se.coderater.entity.Analysis;
import com.se.coderater.entity.Code;
import com.se.coderater.repository.AnalysisRepository;
import com.se.coderater.repository.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource; // 用于加载 classpath 下的资源
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 推荐在服务层方法上使用事务

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AnalysisService {
    // 评分参数 (可以考虑将这些参数外部化到配置文件中)
    private static final double WEIGHT_STYLE = 0.40;
    private static final double WEIGHT_COMPLEXITY = 0.30;
    private static final double WEIGHT_COMMENT = 0.30;

    private static final int STYLE_SCORE_PER_ISSUE_DEDUCTION = 5;
    private static final int STYLE_MAX_ISSUES_FOR_ZERO_SCORE = 20; // 超过20个问题，风格得0分

    private static final int COMPLEXITY_IDEAL_MAX = 5;
    private static final int COMPLEXITY_PENALTY_THRESHOLD = 10;
    private static final int COMPLEXITY_UPPER_LIMIT = 20;
    private static final int COMPLEXITY_DEDUCTION_NORMAL = 10;
    private static final int COMPLEXITY_DEDUCTION_HIGH = 15;

    private static final double COMMENT_RATIO_IDEAL_MIN = 0.10;
    private static final double COMMENT_RATIO_IDEAL_MAX = 0.30;
    private static final int COMMENT_DEDUCTION_PER_PERCENT = 5; // 低于或高于理想区间的每1%扣分

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    private final CodeRepository codeRepository;
    private final AnalysisRepository analysisRepository;

    // Checkstyle 配置文件的路径 (相对于 classpath)
    private static final String CHECKSTYLE_CONFIG_PATH = "checkstyle.xml";

    @Autowired
    public AnalysisService(CodeRepository codeRepository, AnalysisRepository analysisRepository) {
        this.codeRepository = codeRepository;
        this.analysisRepository = analysisRepository;
    }

    @Transactional // 建议将涉及数据库修改的操作放在事务中
    public Analysis analyzeCode(Long codeId) throws IOException, CheckstyleException {
        Code code = codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));

        // 创建临时文件来运行 Checkstyle
        Path tempFilePath = null;
        File tempFile = null;
        try {
            tempFilePath = Files.createTempFile("coderater_temp_", ".java");
            tempFile = tempFilePath.toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(code.getContent());
            }

            // 1. 执行 Checkstyle 分析
            CheckstyleResult checkstyleResult = runCheckstyle(tempFile);
            logger.info("Checkstyle for {}: {} issues found.", code.getFileName(), checkstyleResult.getIssueCount());

            // 2. 使用 JavaParser 分析复杂度、注释等
            // 我们可以直接解析已有的 code.getContent()，避免重复读取文件
            CompilationUnit cu = StaticJavaParser.parse(code.getContent());

            // 计算圈复杂度 (这里我们计算所有方法的平均圈复杂度作为示例)
            // 更复杂的可以是最大圈复杂度，或每个方法的复杂度列表
            CyclomaticComplexityVisitor complexityVisitor = new CyclomaticComplexityVisitor();
            cu.accept(complexityVisitor, null);
            int totalCyclomaticComplexity = complexityVisitor.getTotalComplexity();
            int methodCountForComplexity = complexityVisitor.getMethodCount();
            double averageCyclomaticComplexity = (methodCountForComplexity > 0) ?
                    (double) totalCyclomaticComplexity / methodCountForComplexity : 0.0;
            // 四舍五入到整数，或者你可以让 Analysis 实体中的字段是 double
            int roundedAverageComplexity = (int) Math.round(averageCyclomaticComplexity);
            logger.info("Average Cyclomatic Complexity for {}: {}", code.getFileName(), roundedAverageComplexity);


            // 计算注释行数和非空行数
            long totalLines = code.getContent().lines().count(); // 总行数
            long nonEmptyLines = code.getContent().lines().filter(line -> !line.trim().isEmpty()).count();
            long commentLineCount = cu.getAllComments().stream()
                    .mapToInt(comment -> comment.getRange()
                            .map(range -> range.end.line - range.begin.line + 1)
                            .orElse(0))
                    .sum();
            // 注意：JavaParser 的 comment.getRange() 对于块注释会给出整个块的行数。
            // 对于单行注释，是1行。这种统计方式比简单地按行startsWith("//")更准确。

            double commentRatio = (nonEmptyLines > 0) ? (double) commentLineCount / nonEmptyLines : 0.0;
            logger.info("Comment stats for {}: TotalLines={}, NonEmptyLines={}, CommentLines={}, Ratio={}",
                    code.getFileName(), totalLines, nonEmptyLines, commentLineCount, String.format("%.2f", commentRatio));


            // 3. 创建或更新 Analysis 实体
            // 查找是否已存在该代码的分析，如果存在则更新，否则创建新的
            Analysis analysis = analysisRepository.findByCodeId(codeId)
                    .orElse(new Analysis()); // 如果不存在，则创建一个新的Analysis对象

            analysis.setCode(code);
            analysis.setStyleIssueCount(checkstyleResult.getIssueCount());
            // analysis.setStyleIssuesDetails(checkstyleResult.getDetailedMessagesAsJson()); // 如果需要存储详细信息
            analysis.setCyclomaticComplexity(roundedAverageComplexity);
            analysis.setCommentLineCount((int) commentLineCount);
            analysis.setNonEmptyLineCount((int) nonEmptyLines); // 确保 Code 实体也有这个字段或从这里获取
            analysis.setCommentRatio(Double.parseDouble(String.format("%.2f", commentRatio))); // 保留两位小数

            // 计算各项评分
            int styleScore = calculateStyleScore(analysis.getStyleIssueCount());
            int complexityScore = calculateComplexityScore(analysis.getCyclomaticComplexity());
            int commentScore = calculateCommentScore(analysis.getCommentRatio());

            // 计算总分
            int overallScore = (int) Math.round(
                    (styleScore * WEIGHT_STYLE) +
                            (complexityScore * WEIGHT_COMPLEXITY) +
                            (commentScore * WEIGHT_COMMENT)
            );
            overallScore = Math.max(0, Math.min(100, overallScore)); // 确保在0-100之间

            // 设置瞬时评分字段 (这些不会保存到数据库，仅用于API响应)
            analysis.setStyleScore(styleScore);
            analysis.setComplexityScore(complexityScore);
            analysis.setCommentScore(commentScore);
            analysis.setOverallScore(overallScore);

            logger.info("Scores for {}: Style={}, Complexity={}, Comment={}, Overall={}",
                    code.getFileName(), styleScore, complexityScore, commentScore, overallScore);

            return analysisRepository.save(analysis); // 保存包含原始指标的Analysis对象
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath());
                } else {
                    logger.debug("Temporary file deleted: {}", tempFile.getAbsolutePath());
                }
            }
        }

    }
    private int calculateStyleScore(Integer styleIssueCount) {
        if (styleIssueCount == null) return 0; // 如果没有分析结果，给0分
        if (styleIssueCount == 0) return 100;
        int score = 100 - (styleIssueCount * STYLE_SCORE_PER_ISSUE_DEDUCTION);
        return Math.max(0, score); // 最低0分
        // 或者： return (styleIssueCount >= STYLE_MAX_ISSUES_FOR_ZERO_SCORE) ? 0 : Math.max(0, score);
    }

    private int calculateComplexityScore(Integer avgComplexity) {
        if (avgComplexity == null) return 0;
        if (avgComplexity <= COMPLEXITY_IDEAL_MAX) return 100;
        if (avgComplexity > COMPLEXITY_UPPER_LIMIT) return 0;

        int score = 100;
        if (avgComplexity > COMPLEXITY_IDEAL_MAX && avgComplexity <= COMPLEXITY_PENALTY_THRESHOLD) {
            score -= (avgComplexity - COMPLEXITY_IDEAL_MAX) * COMPLEXITY_DEDUCTION_NORMAL;
        } else { // avgComplexity > COMPLEXITY_PENALTY_THRESHOLD
            score -= (COMPLEXITY_PENALTY_THRESHOLD - COMPLEXITY_IDEAL_MAX) * COMPLEXITY_DEDUCTION_NORMAL;
            score -= (avgComplexity - COMPLEXITY_PENALTY_THRESHOLD) * COMPLEXITY_DEDUCTION_HIGH;
        }
        return Math.max(0, score);
    }

    private int calculateCommentScore(Double commentRatio) {
        if (commentRatio == null) return 0;
        if (commentRatio >= COMMENT_RATIO_IDEAL_MIN && commentRatio <= COMMENT_RATIO_IDEAL_MAX) {
            return 100;
        }

        int score = 100;
        if (commentRatio < COMMENT_RATIO_IDEAL_MIN) {
            // 每低1个百分点扣分
            score -= (int) ((COMMENT_RATIO_IDEAL_MIN - commentRatio) * 100 * COMMENT_DEDUCTION_PER_PERCENT);
        } else { // commentRatio > COMMENT_RATIO_IDEAL_MAX
            // 每高1个百分点扣分 (也可以设置一个上限，例如超过60%直接给低分)
            score -= (int) ((commentRatio - COMMENT_RATIO_IDEAL_MAX) * 100 * COMMENT_DEDUCTION_PER_PERCENT);
        }
        return Math.max(0, score);
    }

    /**
     * 运行 Checkstyle 并返回结果
     */
    private CheckstyleResult runCheckstyle(File javaFile) throws CheckstyleException, IOException {
        File configFile = new ClassPathResource(CHECKSTYLE_CONFIG_PATH).getFile();
        if (!configFile.exists()) {
            throw new IOException("Checkstyle configuration file not found: " + CHECKSTYLE_CONFIG_PATH);
        }

        // 创建 AuditListener 来收集错误
        SimpleAuditListener listener = new SimpleAuditListener();

        Checker checker = new Checker();
        // 设置类加载器，非常重要，否则 Checkstyle 可能找不到它的模块
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        // 加载配置
        checker.configure(ConfigurationLoader.loadConfiguration(
                configFile.getAbsolutePath(), new PropertiesExpander(new Properties())));
        // 添加监听器
        checker.addListener(listener);

        // 处理文件
        List<File> filesToProcess = new ArrayList<>();
        filesToProcess.add(javaFile);
        int errorCount = checker.process(filesToProcess);

        checker.destroy(); // 清理资源

        return new CheckstyleResult(errorCount, listener.getErrors());
    }

    /**
     * 辅助类：用于收集 Checkstyle 错误信息的 AuditListener
     */
    private static class SimpleAuditListener implements AuditListener {
        private final List<String> errors = new ArrayList<>();
        private int issueCount = 0;

        @Override
        public void auditStarted(com.puppycrawl.tools.checkstyle.api.AuditEvent event) {
            // Not used
        }

        @Override
        public void auditFinished(com.puppycrawl.tools.checkstyle.api.AuditEvent event) {
            // Not used
        }

        @Override
        public void fileStarted(com.puppycrawl.tools.checkstyle.api.AuditEvent event) {
            // Not used
        }

        @Override
        public void fileFinished(com.puppycrawl.tools.checkstyle.api.AuditEvent event) {
            // Not used
        }

        @Override
        public void addError(com.puppycrawl.tools.checkstyle.api.AuditEvent event) {
            // 只记录错误和警告级别的问题
            if (event.getSeverityLevel() == SeverityLevel.ERROR || event.getSeverityLevel() == SeverityLevel.WARNING) {
                errors.add(String.format("Checkstyle [%s] %s:%d:%d: %s",
                        event.getSeverityLevel().getName(),
                        event.getFileName(),
                        event.getLine(),
                        event.getColumn(),
                        event.getMessage()));
                issueCount++;
            }
        }

        @Override
        public void addException(com.puppycrawl.tools.checkstyle.api.AuditEvent event, Throwable throwable) {
            errors.add("Checkstyle Exception: " + throwable.getMessage());
            issueCount++; // 也算作一个问题
        }

        public List<String> getErrors() {
            return errors;
        }
        public int getIssueCount() { return issueCount; }
    }

    /**
     * 辅助类：用于封装 Checkstyle 结果
     */
    private static class CheckstyleResult {
        private final int issueCount;
        private final List<String> detailedMessages;

        public CheckstyleResult(int issueCount, List<String> detailedMessages) {
            this.issueCount = issueCount;
            this.detailedMessages = detailedMessages;
        }

        public int getIssueCount() {
            return issueCount;
        }

        public List<String> getDetailedMessages() {
            return detailedMessages;
        }

        // 可选：将详细信息转换为 JSON 字符串以便存储
        // public String getDetailedMessagesAsJson() {
        //     // 使用 Jackson 或 Gson 等库将 detailedMessages 列表转换为 JSON 字符串
        //     // ObjectMapper objectMapper = new ObjectMapper();
        //     // try {
        //     //     return objectMapper.writeValueAsString(detailedMessages);
        //     // } catch (JsonProcessingException e) {
        //     //     logger.error("Error converting Checkstyle messages to JSON", e);
        //     //     return "[]";
        //     // }
        //     return "Implement JSON conversion if needed";
        // }
    }


    /**
     * 辅助类：用于计算圈复杂度的访问者
     * 圈复杂度 V(G) = E - N + 2P  (E: 边数, N: 节点数, P: 连接组件数，通常为1)
     * 简化计算：1 (基础) + (if, for, while, case, catch, &&, ||, ?, -> 数量)
     */
    private static class CyclomaticComplexityVisitor extends VoidVisitorAdapter<Void> {
        private int complexity = 1; // 每个方法的基础复杂度为1
        private int currentMethodComplexity = 0;
        private int totalComplexity = 0;
        private int methodCount = 0;

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            methodCount++;
            currentMethodComplexity = 1; // Reset for new method
            super.visit(md, arg); // Visit children of the method
            totalComplexity += currentMethodComplexity;
            // logger.debug("Method: {}, Complexity: {}", md.getNameAsString(), currentMethodComplexity);
        }

        // 增加复杂度的节点类型
        private void incrementComplexity(Node n) {
            // logger.debug("Complexity point at: {} ({})", n.getClass().getSimpleName(), n.getRange().map(Object::toString).orElse("N/A"));
            currentMethodComplexity++;
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.IfStmt n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ForStmt n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.ForEachStmt n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.WhileStmt n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.DoStmt n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.SwitchEntry n, Void arg) {
            // 每个非 default 的 case 增加复杂度
            if (!n.getLabels().isEmpty()) {
                incrementComplexity(n);
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.stmt.CatchClause n, Void arg) {
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.ConditionalExpr n, Void arg) { // a ? b : c
            incrementComplexity(n);
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.BinaryExpr n, Void arg) { // &&, ||
            if (n.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.AND ||
                    n.getOperator() == com.github.javaparser.ast.expr.BinaryExpr.Operator.OR) {
                incrementComplexity(n);
            }
            super.visit(n, arg);
        }

        // Java 12+ switch expressions with ->
        @Override
        public void visit(com.github.javaparser.ast.expr.SwitchExpr n, Void arg) {
            // 每个 case -> 箭头都会增加复杂度，但 JavaParser 的 AST 结构可能需要更细致的处理
            // 简单起见，可以认为 SwitchExpr 本身贡献一些复杂度，或者遍历其 entries
            // 这里我们先不为 SwitchExpr 本身增加，依赖 SwitchEntry
            super.visit(n, arg);
        }


        public int getTotalComplexity() {
            return totalComplexity;
        }
        public int getMethodCount() { return methodCount; }
    }
}