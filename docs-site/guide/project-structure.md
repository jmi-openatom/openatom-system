# 项目结构详解

## 顶层目录

```
openatom-system/
├── .github/workflows/          # CI/CD 工作流
├── astrbot/                    # AstrBot QQ 机器人
├── backend/                    # 后端 Spring Boot 项目
├── docs/                       # PRD 及 API 文档
├── docs-site/                  # VuePress 开发文档
├── frontend/
│   ├── web_pc/                 # PC 端 Vue 3 项目
│   ├── uni_app/                # UniApp 微信小程序
│   └── ios_app/                # iOS 原生项目（预留）
├── lab-management-system/      # 实验室管理系统后端
├── lab-ui-web/                 # 实验室管理系统前端
├── docker-compose.yml          # 全栈 Docker 编排
├── Dockerfile                  # 后端容器化配置
├── README.md                   # 项目说明
├── DESIGN.md                   # 设计规范
├── BOTPRD.md                   # QQ 机器人需求文档
└── LMS.md                      # 实验室管理系统需求文档
```

## 后端结构 (`backend/`)

```
backend/
├── db/                                 # 手工初始化 SQL
│   ├── create_table.sql                # 全量建表脚本
│   ├── alter_recruitment_form_feature.sql
│   └── cleanup_duplicate_memberships.sql
├── src/main/java/edu/jmi/openatom/server/openatomsystem/
│   ├── OpenatomSystemApplication.java  # 主启动类
│   ├── bootstrap/                      # 启动初始化器
│   │   ├── SystemUserInitializer.java  #   默认管理员
│   │   ├── SystemPermissionInitializer.java # 权限种子
│   │   ├── DefaultClubInitializer.java #   默认社团
│   │   ├── SchemaCompatibilityInitializer.java # 兜底
│   │   └── ...
│   ├── config/                         # 配置类
│   │   ├── SaTokenConfigure.java       #   Sa-Token 配置
│   │   ├── SaPermissionInterfaceImpl.java # 权限接口实现
│   │   ├── MybatisPlusConfig.java      #   MyBatis Plus 配置
│   │   ├── RedisConfig.java            #   Redis 配置
│   │   └── FlywayRepairStrategyConfig.java
│   ├── security/                       # 安全
│   │   └── PasswordService.java        #   密码服务
│   ├── controller/                     # 控制器（44 个）
│   │   ├── AuthController.java         #   认证
│   │   ├── ClubController.java         #   社团
│   │   ├── UserController.java         #   用户
│   │   ├── ApplicationController.java  #   入会申请
│   │   ├── MembershipController.java   #   成员管理
│   │   ├── ActivityController.java     #   活动
│   │   ├── BotManagementController.java #  QQ 群管理
│   │   └── ...
│   ├── service/                        # 服务层
│   │   ├── AuthService.java
│   │   ├── ClubService.java
│   │   ├── UserService.java
│   │   └── ...
│   ├── service/impl/                   # 服务实现
│   ├── mapper/                         # MyBatis Mapper 接口
│   ├── entity/                         # 实体类（60+ 个）
│   ├── dto/                            # 请求 DTO（70+ 个）
│   ├── vo/                             # 响应 VO
│   ├── enums/                          # 枚举
│   │   ├── SystemPermission.java       #   权限点枚举
│   │   ├── UserStatus.java
│   │   └── VoteResultVisibility.java
│   ├── common/                         # 公共工具
│   │   ├── Result.java                 #   统一响应
│   │   ├── Jsons.java                  #   JSON 工具
│   │   ├── Times.java                  #   时间工具
│   │   ├── FormSchemaFields.java       #   表单字段
│   │   └── web/                        #   Web 拦截器
│   │       ├── OperationLogInterceptor.java
│   │       ├── ApplicationSubmitRateLimitInterceptor.java
│   │       └── ...
│   ├── aspect/                         # AOP 切面
│   │   └── ControllerLogAspect.java    #   控制器日志
│   ├── exception/                      # 异常处理
│   │   ├── GlobalExceptionHandlerAdvice.java
│   │   └── RateLimitExceededException.java
│   └── cache/                          # 缓存
├── src/main/resources/
│   ├── application.yml                 # 主配置
│   ├── application-dev.yaml            # 开发环境
│   ├── application-prod.yaml           # 生产环境
│   └── db/migration/                   # Flyway 迁移脚本
│       ├── V1__init_schema.sql
│       ├── V2__add_qq_openid_to_user.sql
│       ├── ...
│       └── V29__add_alumni_group_feature.sql
└── pom.xml                             # Maven 配置
```

## 前端结构 (`frontend/web_pc/`)

```
frontend/web_pc/
├── src/
│   ├── main.ts                         # 应用入口
│   ├── App.vue                         # 根组件
│   ├── api/                            # API 请求
│   │   ├── request.ts                  #   Axios 实例与拦截器
│   │   └── index.ts                    #   API 方法定义
│   ├── router/
│   │   └── index.ts                    #   路由配置（含权限守卫）
│   ├── layouts/                        # 布局
│   │   ├── AdminLayout.vue             #   管理后台布局
│   │   └── SiteLayout.vue             #   前台布局
│   ├── views/                          # 页面视图
│   │   ├── admin/                      #   管理后台（35 个页面）
│   │   │   ├── Dashboard.vue
│   │   │   ├── Users.vue
│   │   │   ├── Clubs.vue
│   │   │   ├── Memberships.vue
│   │   │   ├── Applications.vue
│   │   │   ├── Activities.vue
│   │   │   ├── Interviews.vue
│   │   │   ├── Blogs.vue
│   │   │   ├── Points.vue
│   │   │   ├── Leaves.vue
│   │   │   └── ...
│   │   └── site/                       #   前台展示（30 个页面）
│   │       ├── Home.vue
│   │       ├── Profile.vue
│   │       ├── Recruitment.vue
│   │       ├── ApplicationForm.vue
│   │       ├── Blog.vue
│   │       ├── Activities.vue
│   │       └── ...
│   ├── components/                     # 公共组件
│   │   ├── admin/                      #   管理后台组件
│   │   ├── common/                     #   通用组件
│   │   ├── site/                       #   前台组件
│   │   │   ├── home/                   #   首页组件
│   │   │   ├── shell/                  #   布局壳组件
│   │   │   ├── workspace/              #   工作台组件
│   │   │   └── blog/                   #   博客组件
│   │   └── ui/                         #   动画 UI 组件
│   │       ├── aurora-background/
│   │       ├── apple-card-carousel/
│   │       ├── flip-card/
│   │       ├── globe/
│   │       ├── marquee/
│   │       ├── smooth-cursor/
│   │       └── ...
│   ├── composables/                    # 组合式函数
│   │   ├── useAppStatus.ts
│   │   ├── useRouteTransition.ts
│   │   ├── useTheme.ts
│   │   └── useSiteMotion.ts
│   ├── utils/                          # 工具函数
│   │   ├── auth.ts                     #   认证 Token 管理
│   │   ├── oidc.ts                     #   OIDC 认证
│   │   ├── permission.ts               #   权限判断
│   │   ├── format.ts                   #   格式化
│   │   ├── markdown.ts                 #   Markdown 渲染
│   │   └── qr.ts                       #   二维码
│   ├── constants/                      # 常量
│   ├── plugins/                        # 插件注册
│   ├── styles/                         # 样式
│   ├── lib/                            # 第三方库封装
│   └── vendor/                         # 第三方代码
├── public/                             # 静态资源
├── package.json
├── vite.config.ts
├── tsconfig.json
└── Dockerfile                          # 前端容器化配置
```

## Docker 编排结构

系统通过 `docker-compose.yml` 编排以下服务：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| redis | openatom-redis | 6379 | Redis 缓存，仅本地访问 |
| backend | openatom-backend | 8921 | Spring Boot 后端，host 网络模式 |
| frontend | openatom-frontend | 18080→80 | Vue 前端 + Nginx |
| astrbot | openatom-astrbot | 6185, 6198 | AstrBot 机器人核心 |
| napcat | openatom-napcat | 6099, 3000 | NapCat QQ 协议端 |

## 文档资源

| 文件 | 说明 |
|------|------|
| `README.md` | 项目整体说明与部署指南 |
| `DESIGN.md` | 前端设计规范（Apple 风格设计系统） |
| `BOTPRD.md` | QQ 群机器人需求文档 |
| `LMS.md` | 实验室管理系统需求文档 |
| `docs/club-management-prd.md` | 社团管理系统 PRD |
| `docs/club-management-rest-api.md` | REST API 文档 |
| `docs/club-management-openapi.yaml` | OpenAPI 规范 |
| `docs/oauth-usage.md` | OAuth 使用指南 |
| `docs/ai-activity-automation-prd.md` | AI 活动自动化 PRD |
| `docs/ai-integration-guide.md` | AI 集成指南 |
