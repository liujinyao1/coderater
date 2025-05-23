# CodeRater 后端项目

## 1. 项目概述

CodeRater 是一个旨在帮助用户分析 Java 代码质量并提供反馈的工具。后端系统提供 RESTful API，支持用户认证、代码上传与管理、代码结构解析、代码风格检查、复杂度分析、注释情况评估，并最终给出一个综合评分。

**核心功能:**

*   **用户认证与管理**:
    *   用户注册与登录 (基于 JWT)。
    *   用户可以管理自己上传的代码。
*   **代码上传与解析**: 认证用户可以上传 `.java` 文件，系统将解析其基本结构（类、方法、行数）并与用户关联。
*   **代码质量分析**:
    *   **代码风格检查**: 使用 Checkstyle 根据预设规则（基于 Google Java Style）检查代码规范性。
    *   **复杂度分析**: 计算方法的平均圈复杂度。
    *   **可读性分析**: 评估代码的注释比例。
*   **综合评分**: 基于以上分析指标，给出一个量化的代码质量评分。

**技术栈:**

*   Java 17
*   Spring Boot 3.x
*   Spring Data JPA
*   Spring Security (JWT 认证)
*   MySQL 8.0
*   Maven
*   JavaParser (用于代码结构解析、复杂度及注释分析)
*   Checkstyle (用于代码风格检查)
*   jjwt (Java JWT library)

## 2. 环境准备与运行

### 2.1 所需环境

*   JDK 17 或更高版本
*   Maven 3.8+
*   MySQL 8.0 (确保数据库服务正在运行)
*   IntelliJ IDEA (推荐) 或其他 Java IDE

### 2.2 项目配置

1.  **克隆项目**:
    ```bash
    git clone [你的项目GIT仓库地址]
    cd coderater
    ```
2.  **数据库配置**:
    *   在 MySQL 中创建一个数据库，例如 `coderater_db`:
        ```sql
        CREATE DATABASE coderater_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
        ```
    *   修改 `src/main/resources/application.properties` 文件中的数据库连接信息：
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/coderater_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
        spring.datasource.username=your_mysql_username # 替换为你的MySQL用户名
        spring.datasource.password=your_mysql_password # 替换为你的MySQL密码

        # JWT 配置 (请确保替换为安全的密钥，至少64字节长)
        jwt.secret=YourSuperLongAndVeryVeryVerySecureSecretKeyForHS512AlgorithmAtLeast64BytesLong
        jwt.expiration.ms=86400000 # 24 hours in milliseconds
        ```
3.  **Maven 依赖**:
    项目使用 Maven 管理依赖。首次运行时，IDE 或 Maven 会自动下载所需依赖。

### 2.3 运行项目

*   **使用 IDE**: 直接在 IntelliJ IDEA 中打开项目，找到 `com.se.coderater.CoderaterApplication.java` 主类，右键点击并选择 "Run 'CoderaterApplication'".
*   **使用 Maven 命令行**:
    ```bash
    mvn spring-boot:run
    ```
项目默认运行在 `http://localhost:8080`。

## 3. API 接口文档

**认证相关的请求头**: 对于需要认证的接口，请在请求头中添加 `Authorization` 字段，值为 `Bearer <YOUR_JWT_TOKEN>`。

### 3.1 用户认证

*   **用户注册**
    *   **URL**: `/api/auth/register`
    *   **Method**: `POST`
    *   **Content-Type**: `application/json`
    *   **Request Body**:
        ```json
        {
          "username": "testuser",
          "email": "test@example.com",
          "password": "password123"
        }
        ```
    *   **Success Response (200 OK)**:
        ```json
        {
          "message": "User registered successfully!",
          "accessToken": null
        }
        ```
    *   **Error Responses**:
        *   `400 Bad Request`: 用户名/邮箱已存在，或输入不合法 (包含详细校验错误信息)。
            ```json
            {
                "timestamp": 1678886400000,
                "status": 400,
                "errors": {
                    "password": "Password must be between 6 and 40 characters",
                    "username": "Username must be between 3 and 20 characters",
                    "email": "Email should be valid"
                },
                "message": "Validation failed"
            }
            ```

*   **用户登录**
    *   **URL**: `/api/auth/login`
    *   **Method**: `POST`
    *   **Content-Type**: `application/json`
    *   **Request Body**:
        ```json
        {
          "username": "testuser",
          "password": "password123"
        }
        ```
    *   **Success Response (200 OK)**:
        ```json
        {
          "message": "User logged in successfully!",
          "accessToken": "your_jwt_token_here" // JWT Token
        }
        ```
    *   **Error Responses**:
        *   `401 Unauthorized`: 用户名或密码错误。
            ```json
            {
                "message": "Error: Invalid username or password",
                "accessToken": null
            }
            ```

### 3.2 代码管理 (需要认证)

*   **上传代码**
    *   **URL**: `/api/code/upload`
    *   **Method**: `POST`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Content-Type**: `multipart/form-data`
    *   **Request Body**:
        *   `file`: (类型: File) 需要上传的 `.java` 文件。
    *   **Success Response (201 Created)**:
        ```json
        {
          "id": 1,
          "fileName": "Test.java",
          "content": "public class Test {\n    // ... \n}",
          "uploadedAt": "2025-05-21T13:40:42.0918616",
          "classCount": 1,
          "methodCount": 2,
          "lineCount": 8
          // "uploader" 字段通常不会在此完整序列化以避免循环，但代码已与用户关联
        }
        ```
    *   **Error Responses**:
        *   `400 Bad Request`: 文件无效。
        *   `401 Unauthorized`: 未认证或认证失败。

*   **获取当前用户上传的所有代码**
    *   **URL**: `/api/code/mycode`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Success Response (200 OK)**: 返回一个包含用户所有 `Code` 对象的 JSON 数组，或者提示用户尚未上传代码。
        ```json
        [
          { "id": 1, "fileName": "Test1.java", "classCount": 1, ... },
          { "id": 2, "fileName": "Test2.java", "classCount": 2, ... }
        ]
        ```
        或者 (如果无代码):
        ```json
        "You have not uploaded any code yet."
        ```
    *   **Error Responses**:
        *   `401 Unauthorized`: 未认证。

*   **获取当前用户指定的代码详情**
    *   **URL**: `/api/code/{codeId}`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (代码ID)
    *   **Success Response (200 OK)**: 返回指定 `codeId` 的 `Code` 对象。
    *   **Error Responses**:
        *   `401 Unauthorized`: 未认证。
        *   `404 Not Found`: 代码不存在或不属于当前用户。
            ```json
            {
              "error": "Not Found",
              "message": "Code not found or you do not have permission to view it."
            }
            ```

*   **修改代码文件名 (仅限自己的代码)**
    *   **URL**: `/api/code/{codeId}/filename`
    *   **Method**: `PUT`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (代码ID)
    *   **Request Parameter**: `newFileName` (新的文件名，例如 `UpdatedName.java`)
    *   **Success Response (200 OK)**: 返回更新后的 `Code` 对象。
    *   **Error Responses**:
        *   `400 Bad Request`: 新文件名无效。
        *   `401 Unauthorized`: 未认证。
        *   `403 Forbidden`: 尝试修改不属于自己的代码。
        *   `404 Not Found`: 代码不存在。

*   **删除代码 (仅限自己的代码)**
    *   **URL**: `/api/code/{codeId}`
    *   **Method**: `DELETE`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (代码ID)
    *   **Success Response (200 OK or 204 No Content)**:
        ```json
        {
          "message": "Code with id X deleted successfully."
        }
        ```
    *   **Error Responses**:
        *   `401 Unauthorized`: 未认证。
        *   `403 Forbidden`: 尝试删除不属于自己的代码。
        *   `404 Not Found`: 代码不存在。

### 3.3 代码质量分析与评分 (可配置为需要认证)

*   **URL**: `/api/analysis/{codeId}`
*   **Method**: `POST`
*   **Headers**: (如果配置为需要认证) `Authorization: Bearer <YOUR_JWT_TOKEN>`
*   **Path Variable**:
    *   `codeId`: (类型: Long) 需要分析的代码记录的 ID。
*   **Request Body**: (无)
*   **Success Response (200 OK)**:
    ```json
    {
      "id": 8, // 分析结果在数据库中的ID
      "code": { // 关联的原始代码信息 (部分字段)
        "id": 2,
        "fileName": "Test.java",
        // "content" 字段通常较大，可以考虑在此处省略或部分显示
        "uploadedAt": "2025-05-21T13:44:57.428722",
        "classCount": 1,
        "methodCount": 2,
        "lineCount": 8
      },
      "styleIssueCount": 3,        // 代码风格问题数量 (来自Checkstyle)
      "cyclomaticComplexity": 1,   // 平均圈复杂度 (来自JavaParser)
      "commentRatio": 0.13,        // 注释行数 / 非空行数 的比例 (来自JavaParser)
      "commentLineCount": 1,       // 注释行总数 (来自JavaParser)
      "nonEmptyLineCount": 8,      // 非空行总数
      "analyzedAt": "2025-05-21T17:02:40.6503924", // 分析执行时间
      "overallScore": 79,          // 综合评分 (0-100)
      "styleScore": 85,            // 代码风格单项得分 (0-100)
      "complexityScore": 100,      // 圈复杂度单项得分 (0-100)
      "commentScore": 55           // 注释情况单项得分 (0-100)
    }
    ```
*   **Error Responses**:
    *   `400 Bad Request`: `codeId` 无效或对应的代码记录不存在。
    *   `401 Unauthorized`: (如果配置为需要认证) 未认证。
    *   `403 Forbidden`: (如果配置为需要认证且用户无权分析该代码，或代码不属于该用户)。
    *   `500 Internal Server Error`: 分析过程中发生内部错误。

### 3.4 评分指标详解

*   **`styleIssueCount` (代码风格问题数量)**:
    *   **含义**: Checkstyle 根据 `src/main/resources/checkstyle.xml` 配置文件检查出的代码风格问题的总数。
    *   **影响**: 该值越低越好。

*   **`cyclomaticComplexity` (平均圈复杂度)**:
    *   **含义**: 代码中所有方法圈复杂度的平均值。圈复杂度衡量代码逻辑分支的多少，值越高通常意味着代码越难理解和测试。
    *   **参考范围**:
        *   1-5: 低复杂度，良好。
        *   6-10: 可接受的复杂度。
        *   11-20: 中等复杂度，可能需要关注。
        *   20以上: 高复杂度，建议重构。

*   **`commentRatio` (注释比例)**:
    *   **含义**: 注释行数占非空代码行数的百分比。
    *   **参考范围**:
        *   理想范围通常在 10% - 30% 之间。过低可能表示文档不足，过高（如大量注释掉的代码）也可能不是好现象。

*   **`commentLineCount` (注释行数量)**:
    *   **含义**: 代码中实际的注释行数。

*   **`nonEmptyLineCount` (非空行数量)**:
    *   **含义**: 代码中排除了纯空行后的总行数。

*   **`styleScore` (代码风格单项得分, 0-100)**:
    *   **含义**: 基于 `styleIssueCount` 计算得出。问题越少，得分越高。
    *   **计算简述**: 满分100，每发现一个风格问题扣除一定分数（当前为5分/问题），最低为0分。

*   **`complexityScore` (圈复杂度单项得分, 0-100)**:
    *   **含义**: 基于 `cyclomaticComplexity` (平均圈复杂度) 计算得出。复杂度越低，得分越高。
    *   **计算简述**: 平均圈复杂度在理想值（如<=5）以内得满分。超过理想值后，随着复杂度增加，扣分逐渐增多，超过上限（如20）则得0分。

*   **`commentScore` (注释情况单项得分, 0-100)**:
    *   **含义**: 基于 `commentRatio` (注释比例) 计算得出。
    *   **计算简述**: 注释比例在理想区间（如10%-30%）内得满分。低于或高于此区间，得分会相应降低。

*   **`overallScore` (综合评分, 0-100)**:
    *   **含义**: 根据风格、复杂度、注释三个单项得分及其预设权重综合计算得出。
    *   **计算简述**: `(styleScore * 0.4) + (complexityScore * 0.3) + (commentScore * 0.3)`，结果四舍五入并确保在0-100范围内。
    *   **解读**:
        *   85-100: 优秀
        *   70-84: 良好
        *   60-69: 及格
        *   60以下: 有较大改进空间

## 4. 项目结构


coderater/
├── src/main/java/com/se/coderater/ # Java源代码根目录
│ ├── config/ # 配置类 (SecurityConfig.java, DataInitializer.java - 可选)
│ ├── controller/ # API 控制器 (AuthController.java, CodeController.java, AnalysisController.java)
│ ├── dto/ # 数据传输对象 (RegisterRequest.java, LoginRequest.java, AuthResponse.java)
│ ├── entity/ # JPA 实体类 (User.java, Code.java, Analysis.java)
│ ├── exception/ # 全局异常处理器 (GlobalExceptionHandler.java)
│ ├── repository/ # JPA 仓库接口 (UserRepository.java, CodeRepository.java, AnalysisRepository.java)
│ ├── security/ # Spring Security 相关 (JwtUtils.java, AuthTokenFilter.java, AuthEntryPointJwt.java)
│ └── service/ # 业务逻辑服务 (AuthService.java, UserDetailsServiceImpl.java, CodeService.java, AnalysisService.java)
├── src/main/resources/
│ ├── static/ # 静态资源 (如果需要)
│ ├── templates/ # 视图模板 (如果需要)
│ ├── application.properties # Spring Boot 配置文件
│ └── checkstyle.xml # Checkstyle 规则配置文件
├── pom.xml # Maven 项目配置文件
└── README.md # 本文档

## 5. Checkstyle 配置

代码风格检查规则定义在 `src/main/resources/checkstyle.xml` 文件中。当前配置基于 Google Java Style Guide，并进行了部分调整。团队可以根据需要进一步自定义这些规则。

## 6. 后续开发计划 (参考)

*   **完善用户管理**:
    *   获取当前用户信息 API (`/api/user/me`)。
    *   修改用户信息（如密码）。
    *   (可选) 管理员管理用户功能。
*   **完善代码管理**:
    *   允许用户修改代码内容（并重新触发解析和分析）。
    *   代码分页查询。
*   **完善错误处理和输入验证**。
*   **单元测试与集成测试**: 提高代码覆盖率和系统稳定性。
*   **前端对接**: 与前端团队协作完成整个应用。
*   **部署**: 准备生产环境部署方案。

## 7. 协作

可以直接点击仓库页面的 Fork 按钮，在自己的账号下创建仓库副本，将个人 Fork 克隆到本地，修改后推送到自己的远程仓库，如果想要合并到本仓库可以提交 Pull Request。
