# 后端项目结构

## 包结构总览

后端代码位于 `backend/src/main/java/edu/jmi/openatom/server/openatomsystem/` 下，采用标准的分层包结构：

```
openatomsystem/
├── OpenatomSystemApplication.java   # 主启动类
├── bootstrap/                       # 启动初始化器
├── cache/                           # 缓存
├── common/                          # 公共工具与常量
│   └── web/                         #   Web 拦截器
├── config/                          # 配置类
├── controller/                      # 控制器层
├── dto/                             # 请求 DTO
├── entity/                          # 数据库实体
├── enums/                           # 枚举
├── exception/                       # 异常处理
├── mapper/                          # MyBatis Mapper
├── security/                        # 安全服务
├── service/                         # 服务接口
│   └── impl/                        #   服务实现
├── aspect/                          # AOP 切面
└── vo/                              # 响应 VO
```

## 各包详解

### bootstrap（启动初始化）

负责应用启动时的数据种子初始化：

| 类 | 职责 |
|----|------|
| `SystemUserInitializer` | 初始化默认超级管理员账号 |
| `SystemPermissionInitializer` | 从 `SystemPermission` 枚举同步权限点到数据库 |
| `PermissionSeedCatalog` | 权限种子目录定义 |
| `RoleSeedTemplate` | 角色种子模板 |
| `UserSeedTemplate` | 用户种子模板 |
| `DefaultClubInitializer` | 创建默认社团 |
| `DefaultDepartmentInitializer` | 创建默认部门 |
| `DefaultPositionInitializer` | 创建默认岗位 |
| `DefaultMembershipInitializer` | 创建默认成员关系 |
| `SchemaCompatibilityInitializer` | 数据库 Schema 兼容性兜底 |
| `AvatarMaintenanceInitializer` | 头像存储路径维护 |

### config（配置类）

| 类 | 职责 |
|----|------|
| `SaTokenConfigure` | Sa-Token 配置（拦截器注册、CORS、JWT） |
| `SaPermissionInterfaceImpl` | Sa-Token 权限接口实现（从数据库读取用户权限） |
| `MybatisPlusConfig` | MyBatis Plus 配置（分页插件、自动填充） |
| `RedisConfig` | Redis 序列化配置 |
| `FlywayRepairStrategyConfig` | Flyway 修复策略 |

### common（公共工具）

| 类 | 职责 |
|----|------|
| `Result<T>` | 统一 API 响应封装 |
| `Jsons` | JSON 序列化/反序列化工具 |
| `Times` | 时间处理工具 |
| `FormSchemaFields` | 动态表单字段定义 |
| `PageRequests` | 分页请求工具 |

#### common/web（Web 拦截器）

| 类 | 职责 |
|----|------|
| `OperationLogInterceptor` | 操作日志记录拦截器 |
| `ApplicationSubmitRateLimitInterceptor` | 报名提交限流拦截器 |
| `ClientIpResolver` | 客户端 IP 解析（支持 X-Forwarded-For） |

### controller（控制器层）

共有 44 个 Controller，按业务模块分组：

#### 认证与用户

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `AuthController` | `/auth` | 登录、登出、Token 刷新、当前用户信息 |
| `UserController` | `/users` | 用户 CRUD、角色分配、密码重置、导入 |
| `OidcController` | `/oidc` | OIDC 认证端点 |
| `OauthClientController` | `/oauth` | OAuth 客户端管理 |

#### 社团管理

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `ClubController` | `/clubs` | 社团 CRUD、状态管理 |
| `DepartmentController` | `/clubs/{clubId}/departments` | 部门管理 |
| `PositionController` | `/clubs/{clubId}/positions` | 岗位管理 |
| `MembershipController` | `/memberships` | 成员管理 |
| `ClubRegulationController` | `/regulations` | 社团规章制度 |

#### 招新系统

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `RecruitmentCampaignController` | `/recruitment-campaigns` | 招新计划 |
| `ApplicationController` | `/applications` | 入会申请 |
| `ApprovalController` | `/applications/{id}/approvals` | 审批流程 |
| `InterviewController` | `/interviews` | 面试管理 |

#### 活动与签到

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `ActivityController` | `/activities` | 活动 CRUD、报名 |
| `CheckInController` | `/check-ins` | 内部签到 |
| `AwardController` | `/awards` | 获奖记录 |
| `SchoolCalendarController` | `/school-calendar` | 校历管理 |

#### 办公自动化

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `LeaveApplicationController` | `/leave-applications` | 请假申请 |
| `LeaveAttachmentController` | `/leave-attachments` | 请假附件 |
| `OfficeDocumentController` | `/office-documents` | 文书管理 |
| `DocumentTemplateController` | `/document-templates` | 文书模板 |
| `NotificationController` | `/notifications` | 通知管理 |

#### 互动功能

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `BlogController` | `/blog` | 博客文章、评论、互动 |
| `VoteController` | `/votes` | 投票活动 |
| `LotteryController` | `/lotteries` | 抽奖活动 |
| `PointController` | `/points` | 积分系统 |

#### 站点功能

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `SiteController` | `/site` | 公开信息、注册开关 |
| `PublicController` | `/public` | 公开数据接口 |
| `SiteFormController` | `/site-forms` | 信息收集表单 |
| `FormSubmissionController` | `/form-submissions` | 表单提交 |
| `FileController` | `/files` | 文件上传 |
| `ImageHostingController` | `/image-hosting` | 图床服务 |

#### 系统管理

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `RoleController` | `/roles` | 角色管理 |
| `PermissionController` | `/permissions` | 权限点管理 |
| `LogController` | `/operation-logs`, `/login-logs` | 日志查询 |
| `DataOpenController` | `/data-open` | 数据开放平台 |
| `ShowcaseAppController` | `/showcase-apps` | 应用展示 |
| `AlumniGroupController` | `/alumni-groups` | 校友分组 |

#### AI 与机器人

| Controller | 路由前缀 | 说明 |
|------------|----------|------|
| `AiActivityAutomationController` | `/ai-activity` | AI 活动自动化 |
| `AiSettingsController` | `/ai/settings` | AI 设置 |
| `BotManagementController` | `/bot-management` | QQ 群管理 |
| `BotUserLookupController` | `/bot` | 机器人用户查询 |

### entity（实体类）

60+ 个实体类，与数据库表一一对应。主要实体包括：

- **用户体系**：`User`, `UserRole`, `Role`, `RolePermission`, `Permission`
- **社团体系**：`Club`, `ClubDepartment`, `ClubPosition`, `ClubMembership`, `ClubPositionRole`
- **招新体系**：`MembershipApplication`, `ApprovalRecord`, `Interview`, `InterviewFeedback`, `InterviewInterviewer`, `RecruitmentCampaign`
- **活动体系**：`ClubActivity`, `ActivityRegistration`, `ClubAward`
- **签到体系**：`CheckInSession`, `CheckInGroup`, `CheckInGroupMember`, `CheckInRecord`, `CheckInTarget`, `CheckInExclusion`
- **办公体系**：`LeaveApplication`, `OfficeDocument`, `DocumentTemplate`, `GeneratedDocument`, `Notification`, `NotificationReceiver`
- **互动体系**：`BlogArticle`, `BlogComment`, `BlogArticleInteraction`, `VoteCampaign`, `VoteOption`, `VoteRecord`, `LotteryCampaign`, `LotteryPrize`, `LotteryWinner`
- **积分体系**：`PointAccount`, `PointTransaction`, `PointRedeemItem`, `PointRedemption`
- **系统体系**：`SystemSetting`, `OperationLog`, `LoginLog`, `SchoolCalendarSetting`, `SchoolCalendarAdjustment`
- **OAuth 体系**：`OauthClient`, `OauthAuthorizationCode`
- **其他**：`SiteForm`, `FormSubmission`, `ImageHostingAsset`, `ShowcaseApp`, `DataOpenApplication`, `ClubRegulation`, `ClubAlumniGroup`, `ExitApplication`, `EveningStudySchedule`, `AiActivitySession`, `AiActivityPlan`, `AiActivityMessage`, `AiCallLog`

### enums（枚举）

| 枚举 | 说明 |
|------|------|
| `SystemPermission` | 系统权限点定义（100+ 权限点） |
| `UserStatus` | 用户状态（ACTIVE / DISABLED / DELETED） |
| `VoteResultVisibility` | 投票结果可见性 |

### exception（异常处理）

| 类 | 职责 |
|----|------|
| `GlobalExceptionHandlerAdvice` | 全局异常处理器，统一异常 → `Result` 转换 |
| `RateLimitExceededException` | 限流超限异常 |

### aspect（AOP 切面）

| 类 | 职责 |
|----|------|
| `ControllerLogAspect` | Controller 方法日志切面 |

## Maven 依赖

核心依赖详见 `backend/pom.xml`：

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Sa-Token 认证 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.37.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.37.0</version>
</dependency>

<!-- MyBatis Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```
