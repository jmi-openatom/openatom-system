

# 实验室管理系统（Lab Management System）

## 产品需求文档 (PRD)

| **项目名称** | **实验室管理系统 (LMS)** | **版本号** | **V1.0.0** |
| -------- | ----------------- | ------- | ---------- |
| 依赖系统     | 社团管理系统 (CMS)      | 编写时间    | 2026-06-11 |
| 文档状态     | 评审通过/交付开发         | 编写人     | 产品团队       |

## 1. 项目概述与建设目标

### 1.1 背景

当前社团管理系统（CMS）已具备完善的用户组织架构与基础身份管理。为了满足算法实验室（ACM/蓝桥杯方向）的日常训练、考勤及量化考核需求，现独立建设“实验室管理系统”。该系统独立运行，但底层账号、实验室成员资格认证完全依赖社团系统。

### 1.2 核心目标

- **统一认证：** 采用 OAuth2.0，复用社团系统账号，实现无缝登录。

- **权限受控：** 实验室成员名单及准入权限由社团系统后台统一分发。

- **AI 赋能：** 自动生成符合 ACM 赛制的每日一练题目，并配备独立沙箱判题（OJ）核心。

- **独立闭环：** 考勤签到、通知中心、内部积分扣减在实验室后台独立闭环；正常签到奖励异步对接社团积分。

## 2. 系统总体架构与数据流向

系统采用前后端分离架构。项目目录与技术栈选型参考社团系统（推荐：`Frontend: Vue3/React + TailwindCSS`, `Backend: Spring Boot / Django / Go`）。

```
+-------------------------------------------------------------+
|                     社团管理系统 (CMS)                       |
|  [用户主库] -> [OAuth 认证中心] -> [实验室成员打标] -> [积分接口]  |
+-------------------------------------------------------------+
                               | (OAuth / API)
                               v
+-------------------------------------------------------------+
|                     实验室管理系统 (LMS)                       |
|  [独立前端] -> [网关/业务后端] -> [AI 题目生成] -> [沙箱评测机]  |
|  [独立本地库 (用户冗余表、题目表、签到流水、通知表、积分表)]       |
+-------------------------------------------------------------+
```

## 3. 功能模块详细需求

### 3.1 账号互通与鉴权模块 (Auth Module)

- **需求描述：** 用户无需在实验室系统注册，直接通过“社团账号登录”。

- **业务规则：**
  
  1. 点击登录，重定向至 CMS OAuth 授权页。
  
  2. CMS 返回 `Access Token`，LMS 解析 Token 并请求 CMS 用户信息接口。
  
  3. **权限截断（关键）：** 读取用户信息中的 `is_lab_member` 字段。
     
     - 若为 `true`：允许登录。首次登录则在 LMS 本地 `lab_users` 表自动生成记录（同步更新头像、昵称）；非首次登录则更新登录态，发放 LMS 本地 JWT。
     
     - 若为 `false`：拦截登录，跳转至错误页，提示：“您当前非实验室正式成员，请联系管理员在社团管理系统开通权限。”

### 3.2 AI 每日一练与 ACM 判题模块 (OJ Module)

- **需求描述：** 引入大语言模型（LLM）API，每日自动生成算法题，并支持代码在线评测。

- **具体流程与规则：**
  
  - **AI 定时生成：** 每日 00:00，LMS 后端通过 Cron 任务调用 AI 接口。通过严格的 JSON Schema 提示词约束，获取：题目名、Markdown描述、时空限制、标准测试用例、C++/Java/Python标准答案。
  
  - **ACM 判题机制：** 用户可在前端切换语言并提交代码。后端将代码和测试用例送入隔离的**沙箱容器**中运行。
  
  - **状态返回：** 严格对比标准输出。状态包括：`AC (通过)`、`WA (答案错误)`、`TLE (超时)`、`MLE (超内存)`、`RE (运行时错误)`、`CE (编译错误)`。

### 3.3 独立签到与考勤模块 (Check-In Module)

- **需求描述：** 实验室成员日常出勤考核，支持与做题联动。

- **业务规则：**
  
  - **通过联动签到（默认）：** 用户在当日 23:59 前，AC 成功当天的“每日一练”，系统自动触发今日签到，状态标记为“通过刷题自动签到”。
  
  - **普通现场签到（备用）：** 后台可生成“动态限时二维码”或限制“实验室局域网 IP 段”，学生到现场后点击签到。

### 3.4 积分与惩罚体系 (Score Module)

- **需求描述：** 结合考勤状态，执行内部扣分惩罚与社团外部加分奖励。

- **业务规则：**
  
  1. **正常签到（跨系统联动）：** 触发签到成功后，LMS 将该事件推入消息队列，异步调用 CMS 提供的 `add_club_score` 接口，为该用户在社团系统增加 X 积分。
  
  2. **迟到/旷课（内部扣分）：**
     
     - 若后台设置了标准现场签到时间（如 09:00），09:01-09:30 签到视为“迟到”，触发 LMS 内部扣分（如 -5 分）。
     
     - 当日结束未有任何签到记录，触发 LMS 内部扣分（如 -10 分）。
     
     - **注意：** 内部扣分仅在 LMS 数据库中记录，影响“实验室信誉分”，不扣除社团大系统的积分。

### 3.5 通知中心模块 (Notification Module)

- **需求描述：** 负责系统内所有关键事件的触达与告知。

- **业务规则：**
  
  - **通知类型：** 站内信（必选）、邮件/微信公众号推送（可选，基于从 CMS 冗余过来的联系方式）。
  
  - **触发场景一览：**
    
    - *题目刷新：* 每日 00:00 AI 题目生成成功后，群发站内信：“今日算法挑战已更新，请及时前往 AC 并完成签到。”
    
    - *违规警告：* 定时任务判定迟到或旷课扣分后，单发个人：“因您今日未按时出勤，系统已扣除实验室信誉分 X 分，当前剩余 Y 分。”
    
    - *同步反馈：* “您的今日签到加分已成功同步至社团管理系统（+X 积分）。”

## 4. 完全对齐社团系统的项目目录结构 (Project Directory Structure)

为保证代码规范、工程解耦及独立部署，本系统的目录结构**完全参照社团系统的微服务/多模块规范进行标准复刻**。

### 4.1 后端项目目录 (Backend Directory)

Plaintext

```
lab-management-system/ (实验室系统后端根目录)
├── lab-admin/              # 完全独立的后台管理系统接口模块
│   ├── src/main/java/com/lab/web/controller/system/    # 后台专有控制器
│   └── src/main/java/com/lab/web/controller/monitor/   # 评测机与AI状态监控
├── lab-framework/          # 核心核心配置 (对齐社团系统的Security、Redis、OAuth2拦截器)
│   ├── config/             # OAuth2Client配置、沙箱安全配置
│   └── security/           # JWT统一鉴权、is_lab_member 门槛拦截器
├── lab-modules/            # 独立业务模块
│   ├── lab-oj/             # OJ评测模块 (包含AI对接逻辑、沙箱调用、题目管理)
│   ├── lab-checkin/        # 独立签到模块 (签到逻辑、考勤打卡)
│   ├── lab-score/          # 积分模块 (本地扣分、异步调用社团积分API的HttpClient/MQ)
│   └── lab-notice/         # 通知中心模块 (站内信、邮件发送、通知模板引擎)
├── lab-common/             # 工具类与通用常量 (对齐社团系统的异常处理与统一返回体)
└── pom.xml / go.mod        # 独立的依赖管理
```

### 4.2 前端项目目录 (Frontend Directory)

Plaintext

```
lab-ui-web/ (实验室系统独立前端)
├── src/
│   ├── api/                # 接口请求层
│   │   ├── oj.js           # AI题目及判题接口
│   │   ├── checkin.js      # 签到接口
│   │   ├── score.js        # 积分流水分页接口
│   │   └── notice.js       # 通知中心接口
│   ├── views/              # 视图层 (区分前台与独立后台)
│   │   ├── index/          # 前台学生端门户
│   │   │   ├── problem/    # 每日一练刷题界面 (包含CodeMirror/Monaco代码编辑器)
│   │   │   └── checkin/    # 学生签到与积分查看页
│   │   └── admin/          # 完全独立的后台管理面板 (对齐社团系统的Element/AntD样式)
│   │       ├── problem-set/# 题目审核与用例管理
│   │       ├── attendance/ # 独立考勤与惩罚名单
│   │       └── scoreboard/ # 实验室信誉分看板
│   ├── store/              # 状态管理 (存储通过OAuth换取的本地JWT及用户信息)
│   └── router/             # 路由配置 (包含独立后台的权限路由守卫)
```

## 5. 核心数据库表关联设计 (Database Design)

系统采用独立数据库 `db_lab_management`，不直接读取 CMS 数据库，仅通过 `club_user_id` 进行逻辑关联。

### 5.1 实验室用户冗余及本地权限表 (`lab_users`)

SQL

```
CREATE TABLE `lab_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '实验室本地主键',
  `club_user_id` bigint(20) NOT NULL COMMENT '对应社团系统的用户ID (唯一索引，用以互通)',
  `username` varchar(64) DEFAULT NULL COMMENT '冗余社团用户名',
  `lab_role` tinyint(2) DEFAULT '0' COMMENT '实验室内部身份: 0-学生, 1-助教, 2-主教练',
  `reputation_score` int(11) DEFAULT '100' COMMENT '实验室内部基础信誉分 (默认100，用于扣减)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_club_uid` (`club_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.2 考勤与积分同步流水表 (`lab_checkin_score_log`)

SQL

```
CREATE TABLE `lab_checkin_score_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '对应lab_users.id',
  `checkin_date` date NOT NULL COMMENT '考勤日期',
  `attendance_status` tinyint(2) DEFAULT '0' COMMENT '0-未签到(旷课), 1-正常(刷题AC), 2-正常(现场), 3-迟到',
  `local_score_change` int(11) DEFAULT '0' COMMENT '实验室内部信誉分变动 (如: -5, -10)',
  `club_sync_status` tinyint(2) DEFAULT '0' COMMENT '社团积分同步状态: 0-无需同步, 1-待同步, 2-同步成功, 3-同步失败',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 6. 非功能性需求

1. **安全性（沙箱隔离）：** 运行 ACM 代码的判题机必须使用 Isolate 或 Docker 容器限制网络、文件读写、最大执行时间和最大内存，防止恶意提交破坏服务器。

2. **高可用（异步解耦）：** 调用社团系统的加分接口、AI大模型的题目生成接口、判题机的评测请求，**必须全部采用异步队列（MQ或线程池）处理**，绝不能阻塞用户的主登录和签到流程。

3. **一致性：** 若社团管理员在 CMS 中移除了某个用户的实验室成员标签，该用户在下一次登录 LMS 时，LMS 必须拒绝发放 JWT 并强制清除其本地 Token。

4. 页面失去焦点监听就直接收卷


