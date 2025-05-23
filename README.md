# CodeRater 后端项目

## 1. 项目概述

CodeRater 是一个旨在帮助用户分析 Java 代码质量并提供反馈的工具。后端系统提供 RESTful API，支持用户认证、代码上传与管理、代码结构解析、代码风格检查、复杂度分析、注释情况评估，并最终给出一个综合评分。

**核心功能:**

*   **用户认证与管理**:
    *   用户注册与登录 (基于 JWT)。
    *   用户可以管理自己上传的代码（查看列表、查看详情、修改文件名、删除）。
    *   （已实现基础）获取当前用户信息。
*   **代码上传与解析**: 认证用户可以上传 `.java` 文件，系统将解析其基本结构（类、方法、行数）并与用户关联。
*   **代码质量分析**:
    *   **代码风格检查**: 使用 Checkstyle 根据预设规则（基于 Google Java Style）检查代码规范性。
    *   **复杂度分析**: 计算方法的平均圈复杂度。
    *   **可读性分析**: 评估代码的注释比例。
*   **综合评分**: 基于以上分析指标，给出一个量化的代码质量评分。每次请求分析都会触发全新的分析过程。
*   **公开代码列表**: 提供一个公开的API，用于展示所有已上传代码的摘要信息列表（分页）。

**技术栈:**

*   Java 17
*   Spring Boot 3.x (你项目中实际使用的版本，例如 3.4.5)
*   Spring Data JPA
*   Spring Security (JWT 认证)
*   MySQL 8.0
*   Maven
*   JavaParser (用于代码结构解析、复杂度及注释分析)
*   Checkstyle (用于代码风格检查, e.g., 10.17.0)
*   jjwt (Java JWT library, e.g., 0.11.5)
*   Lombok

## 2. 环境准备与运行

### 2.1 所需环境

*   JDK 17 或更高版本
*   Maven 3.8+
*   MySQL 8.0 (确保数据库服务正在运行)
*   IntelliJ IDEA (推荐) 或其他 Java IDE
*   API 测试工具 (如 Postman)

### 2.2 项目配置

1.  **克隆项目**:
    ```bash
    git clone [你的项目GIT仓库地址]
    cd coderater # 或者你的项目根目录名
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

        # JWT 配置 (请确保替换为安全的密钥，至少64字节长，特别是使用HS512时)
        jwt.secret=YourSuperLongAndVeryVeryVerySecureSecretKeyForHS512AlgorithmAtLeast64BytesLong
        jwt.expiration.ms=86400000 # 24 hours in milliseconds
        ```
3.  **Maven 依赖**:
    项目使用 Maven 管理依赖。首次在 IDE 中打开项目或执行 Maven 命令时，会自动下载所需依赖。如果遇到问题，可以尝试在项目根目录执行 `mvn clean install -U`。

### 2.3 运行项目

*   **使用 IDE**:
    1.  在 IntelliJ IDEA 中打开项目。
    2.  确保项目被正确识别为 Maven 项目 (右侧 Maven 工具栏应能看到项目结构)。如有必要，点击 "Reload All Maven Projects"。
    3.  确保 IntelliJ IDEA 的 "Delegate IDE build/run actions to Maven" 选项已勾选 (`File -> Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner`)。
    4.  找到 `com.se.coderater.CoderaterApplication.java` 主类。
    5.  右键点击该文件并选择 "Run 'CoderaterApplication.main()'"。
*   **使用 Maven 命令行**:
    在项目根目录下执行：
    ```bash
    mvn spring-boot:run
    ```
项目默认运行在 `http://localhost:8080`。

## 3. API 接口文档

**认证相关的请求头**: 对于需要认证的接口，请在请求头中添加 `Authorization` 字段，值为 `Bearer <YOUR_JWT_TOKEN>`。

### 3.1 用户认证 (`/api/auth`)

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
            // 示例：输入不合法
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

### 3.2 公开代码接口 (`/api/code`)（用于主页显示代码列表）

*   **获取所有代码摘要列表 (分页)**
    *   **URL**: `/api/code/public/list`
    *   **Method**: `GET`
    *   **Query Parameters (可选)**:
        *   `page`: 页码 (从0开始，默认0)
        *   `size`: 每页数量 (默认10)
        *   `sort`: 排序字段和方向 (例如: `uploadedAt,desc` 或 `fileName,asc`)
    *   **Success Response (200 OK)**: 返回分页的 `CodeSummaryDTO` 列表。
        ```json
        {
            "content": [
                {
                    "id": 1,
                    "fileName": "MyClass.java",
                    "uploaderUsername": "userA",
                    "uploadedAt": "2025-05-23T10:00:00",
                    "lineCount": 50
                },
                // ... more code summaries
            ],
            "pageable": {
                "pageNumber": 0,
                "pageSize": 10,
                // ... more paging info
            },
            "totalPages": 5,
            "totalElements": 48,
            // ... more paging info
        }
        ```
    *   **权限**: `permitAll`

### 3.3 用户代码管理 (`/api/code`) (需要认证)

*   **上传代码**
    *   **URL**: `/api/code/upload`
    *   **Method**: `POST`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Content-Type**: `multipart/form-data`
    *   **Request Body (form-data)**:
        *   `file`: (类型: File) 需要上传的 `.java` 文件。
    *   **Success Response (201 Created)**: 返回创建的 `Code` 对象（包含解析的统计数据）。
        ```json
        {
          "id": 1,
          "fileName": "Test.java",
          "content": "public class Test {\n    // ... \n}",
          "uploadedAt": "2025-05-21T13:40:42.0918616",
          "classCount": 1,
          "methodCount": 2,
          "lineCount": 8,
          "uploader": null // 通常 @JsonBackReference 会阻止序列化，或只显示ID
        }
        ```
    *   **Error Responses**: `400 Bad Request`, `401 Unauthorized`.

*   **获取当前用户上传的所有代码**
    *   **URL**: `/api/code/mycode`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Success Response (200 OK)**: 返回一个包含用户所有 `Code` 对象的 JSON 数组。
    *   **Error Responses**: `401 Unauthorized`.

*   **获取当前用户指定的代码详情 (仅限自己的代码)**
    *   **URL**: `/api/code/{codeId}`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (代码ID)
    *   **Success Response (200 OK)**: 返回指定 `codeId` 的 `Code` 对象。
    *   **Error Responses**: `401 Unauthorized`, `403 Forbidden` (非代码所有者), `404 Not Found` (代码不存在)。

*   **修改代码文件 (仅限自己的代码)**
    *   **URL**: `/api/code/{codeId}`
    *   **Method**: `PUT`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (类型: Long) - 需要修改的代码记录的 ID。
    *   **Request Body**:
  ```json
  {
    "fileName": "UpdatedDemo.java",
    "content": "public class UpdatedDemo {\n    public static void main(String[] args) {\n        System.out.println(\"Content has been updated!\");\n    }\n}"
  }
  ```

*   **Success Response (200 OK)**:
  ```json
  {
    "id": 2,
    "fileName": "UpdatedContentAndName.java",
    "content": "public class UpdatedContent {\n  // new simple content\n}",
    "uploadedAt": "2025-05-23T23:03:27.321785",
    "classCount": 1,
    "methodCount": 0,
    "lineCount": 3
  }
  ```
*   **Error Responses**: `401 Unauthorized`, `403 Forbidden` (非代码所有者), `404 Not Found` (代码不存在),`500 Internal Server Error`(服务器内部错误,例如,重新解析新代码内容时发生意外）
*   **删除代码 (仅限自己的代码)**
    *   **URL**: `/api/code/{codeId}`
    *   **Method**: `DELETE`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (代码ID)
    *   **Success Response (200 OK or 204 No Content)**.
    *   **Error Responses**: `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.

### 3.4 代码质量分析与评分 (`/api/analysis`) (需要认证，仅限代码所有者)

*   **触发分析并获取结果**
    *   **URL**: `/api/analysis/{codeId}`
    *   **Method**: `POST`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Path Variable**: `codeId` (需要分析的代码记录的 ID)
    *   **Request Body**: (无)
    *   **Success Response (200 OK)**: 返回 `Analysis` 对象，包含各项指标和评分。
        ```json
        {
          "id": 1, // 分析结果ID
          "code": { "id": 6, "fileName": "MyClass.java", ... }, // 关联的代码摘要
          "styleIssueCount": 5,
          "cyclomaticComplexity": 3,
          "commentRatio": 0.25,
          "commentLineCount": 10,
          "nonEmptyLineCount": 40,
          "analyzedAt": "2025-05-23T21:00:00",
          "styleScore": 75,
          "complexityScore": 100,
          "commentScore": 100,
          "overallScore": 89
        }
        ```
    *   **Error Responses**: `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `500 Internal Server Error`.

### 3.5 用户个人信息 (`/api/user`) (需要认证)（用于个人主页）

*   **获取当前用户信息**
    *   **URL**: `/api/user/me`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
    *   **Success Response (200 OK)**: 返回 `UserProfileDTO`。
        ```json
        {
            "id": 1,
            "username": "testuser",
            "email": "test@example.com",
            "roles": ["ROLE_USER"]
        }
        ```
    *   **Error Responses**: `401 Unauthorized`.

### 3.6 评分指标详解

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



```
coderater/
├── src/main/java/com/se/coderater/ # Java源代码根目录
│ ├── config/ # 配置类 (SecurityConfig.java, DataInitializer.java - 可选)
│ ├── controller/ # API 控制器 (AuthController.java, CodeController.java, AnalysisController.java, UserController.java)
│ ├── dto/ # 数据传输对象 (RegisterRequest.java, LoginRequest.java, AuthResponse.java, CodeSummaryDTO.java, UserProfileDTO.java)
│ ├── entity/ # JPA 实体类 (User.java, Code.java, Analysis.java)
│ ├── exception/ # 全局异常处理器 (GlobalExceptionHandler.java)
│ ├── repository/ # JPA 仓库接口 (UserRepository.java, CodeRepository.java, AnalysisRepository.java)
│ ├── security/ # Spring Security 相关 (JwtUtils.java, AuthTokenFilter.java, AuthEntryPointJwt.java)
│ └── service/ # 业务逻辑服务 (AuthService.java, UserDetailsServiceImpl.java, CodeService.java, AnalysisService.java, UserService.java)
├── src/main/resources/
│ ├── application.properties # Spring Boot 配置文件
│ └── checkstyle.xml # Checkstyle 规则配置文件
├── pom.xml # Maven 项目配置文件
└── README.md # 本文档
```

## 5. Checkstyle 配置

代码风格检查规则定义在 `src/main/resources/checkstyle.xml` 文件中。当前配置基于 Google Java Style Guide，并进行了部分调整。团队可以根据需要进一步自定义这些规则。

## 6. 后续开发计划 (参考)

*   **完善用户管理**:
    *   修改用户信息（如密码）。
    *   (可选) 管理员管理用户功能。
*   **完善代码管理**:
    *   允许用户修改代码内容（并重新触发解析和分析）。
*   **完善错误处理和输入验证** (已通过全局异常处理器进行基础处理)。
*   **单元测试与集成测试**: 提高代码覆盖率和系统稳定性。
*   **前端对接**: 与前端团队协作完成整个应用。
*   **部署**: 准备生产环境部署方案。

## 7. 协作

可以直接点击仓库页面的 Fork 按钮，在自己的账号下创建仓库副本，将个人 Fork 克隆到本地，修改后推送到自己的远程仓库，如果想要合并到本仓库可以提交 Pull Request。
