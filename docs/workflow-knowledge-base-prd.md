# 工作流与知识库系统 PRD

## 1. 文档信息

| 项目名称 | 工作流与知识库系统 | 版本号 | V1.0.0 |
| --- | --- | --- | --- |
| 依赖系统 | OpenAtom 社团管理系统、QQ 机器人、通知中心 | 输出日期 | 2026-06-16 |
| 文档状态 | 需求草案 | 适用范围 | 社团后台、门户、小程序、QQ 机器人 |

## 2. 项目背景

OpenAtom System 已具备用户、社团、部门、岗位、招新、活动、请假、文书、通知、数据开放、QQ 机器人等模块。现有业务中已经存在多个“流程型”场景，例如入会审核、请假审批、退社确认、数据开放申请、活动报名审核、公文审批等，但各模块的审批状态、审批节点、处理人、通知规则分散实现，后续新增流程时需要重复开发。

同时，社团运营过程中存在大量可复用知识，包括社团制度、报名说明、活动手册、实验室文档、常见问题、机器人问答素材、后台操作手册、接口使用说明等。这些内容目前分散在公告、博客、文档、群聊和人工回复中，缺少统一沉淀、检索、版本管理和对机器人问答的供给能力。

因此需要建设“工作流与知识库系统”，将流程编排、审批执行、知识沉淀、智能检索和机器人问答统一纳入 OpenAtom 平台。

## 3. 产品目标

- 建立统一的流程定义、流程实例、节点任务和审批记录模型。
- 支持管理员通过后台配置常见审批流，减少新增业务的重复开发成本。
- 为入会、请假、退社、数据开放、活动报名、公文审批等场景提供统一流程底座。
- 建立统一知识库，支持文档录入、分类、标签、版本、权限、检索和发布。
- 支持知识库向门户、小程序、后台和 QQ 机器人提供统一内容查询能力。
- 预留 AI 检索增强问答能力，让机器人和后台助手能够基于知识库回答问题。
- 保留现有业务模块能力，分阶段迁移，不影响已上线功能稳定运行。

## 4. 用户角色

### 4.1 普通用户/社员

- 查看公开知识库文章、制度、FAQ。
- 提交与自己相关的流程申请。
- 查看本人流程进度、审批意见和处理结果。
- 在门户、小程序或 QQ 机器人中查询知识内容。

### 4.2 申请人

- 发起入会、请假、数据开放、活动报名、文书申请等流程。
- 补充材料、撤回申请、重新提交申请。
- 接收流程节点通知和最终结果通知。

### 4.3 审批人/处理人

- 查看待办任务。
- 审批通过、驳回、退回补充、转交、加签。
- 填写审批意见、上传附件、查看历史流转记录。
- 批量处理低风险或同类任务。

### 4.4 知识库编辑

- 新建、编辑、下线知识条目。
- 管理分类、标签、附件和版本。
- 提交文章发布审核。
- 查看知识命中次数、机器人引用次数和用户反馈。

### 4.5 社团管理员/干部

- 配置本社团可用流程模板。
- 指定节点审批人、部门负责人、角色负责人和超时规则。
- 管理本社团知识库空间。
- 查看流程统计、知识库统计和异常任务。

### 4.6 系统管理员

- 管理全局流程模板、流程权限、知识库空间和系统参数。
- 配置 AI 检索、向量索引、机器人接入、审计日志。
- 处理跨社团流程异常和敏感知识内容。

## 5. 业务范围

### 5.1 本期范围

- 工作流模板管理。
- 流程节点配置。
- 流程实例发起、审批、流转、撤回、终止。
- 待办、已办、抄送、我发起的流程列表。
- 流程通知与操作日志。
- 知识库空间、分类、标签、文章、附件管理。
- 知识库全文检索和权限控制。
- FAQ 管理与门户/后台展示。
- QQ 机器人知识查询接口。
- 基础 AI 问答预留接口与检索结果引用。

### 5.2 暂不纳入本期

- 复杂 BPMN 可视化建模器。
- 表达式级流程脚本沙箱。
- 多租户商业化计费。
- 大规模企业级文档协同编辑。
- 私有大模型训练。

### 5.3 后续扩展

- AI 活动自动化：用户输入活动需求后，由 DeepSeek 多轮澄清、生成活动建议和策划案，并基于预上传 Word 模板生成活动申请书、活动申请表、志愿者申请表。详细需求见 `docs/ai-activity-automation-prd.md`。
- 可视化流程编排画布。
- 表单设计器与流程节点深度绑定。
- AI 自动生成流程摘要、审批意见建议、知识条目摘要。
- 知识库向量检索与多轮追问。
- 知识内容质量评分、过期提醒、自动巡检。
- 企业微信、飞书、钉钉等外部审批与通知集成。

## 6. 核心业务流程

### 6.1 流程模板配置流程

1. 管理员进入“工作流管理”。
2. 新建流程模板，填写名称、编码、适用业务、所属社团、启用状态。
3. 配置发起权限，例如全部用户、社员、部门负责人、指定角色。
4. 配置节点列表，包括节点名称、节点类型、处理人规则、操作权限和超时规则。
5. 配置节点动作，包括通过、驳回、退回补充、转交、加签、终止。
6. 配置通知模板，包括发起成功、节点待办、审批完成、驳回、超时提醒。
7. 发布模板后，业务模块可按模板编码发起流程实例。

### 6.2 流程发起流程

1. 用户在业务页面提交申请表单。
2. 业务模块保存业务单据，例如请假单、入会申请、公文、数据开放申请。
3. 业务模块调用工作流服务，传入 `businessType`、`businessId`、`templateCode`、`starterUserId`、摘要信息。
4. 工作流服务创建流程实例，生成首个节点任务。
5. 系统按节点处理人规则生成待办任务并发送通知。
6. 用户可在“我发起的”中查看流程状态和流转记录。

### 6.3 审批流转流程

1. 审批人在“待办中心”打开任务。
2. 系统展示业务详情、申请人信息、流程图、历史审批意见、附件。
3. 审批人选择动作并填写意见。
4. 工作流服务校验当前任务状态和处理人权限。
5. 当前任务完成，系统写入操作记录。
6. 系统根据节点配置生成下一节点任务，或将流程置为完成/驳回/终止。
7. 系统回调业务模块，更新业务单据状态。
8. 系统发送站内信，必要时同步 QQ 机器人提醒。

### 6.4 知识库发布流程

1. 编辑创建知识条目，选择空间、分类、标签和可见范围。
2. 编辑录入 Markdown/富文本正文，上传附件。
3. 系统保存为草稿版本。
4. 编辑提交发布。
5. 若空间开启审核，则进入知识发布审核流程。
6. 审核通过后，条目变为已发布，并生成可检索索引。
7. 若接入向量检索，系统异步进行文本切片和向量化。
8. 门户、后台、机器人可查询已发布且有权限访问的知识内容。

### 6.5 机器人知识问答流程

1. QQ 群用户发送问题或机器人指令。
2. AstrBot 插件将问题、群号、用户标识发送到 OpenAtom 后端。
3. 后端根据群配置识别可访问的知识库空间。
4. 系统执行关键词检索，后续可扩展向量检索。
5. 系统返回匹配的 FAQ、文章摘要和引用链接。
6. 若启用 AI 问答，系统将检索片段作为上下文生成回答，并附带来源。
7. 机器人将回答发送到群聊或私聊。

## 7. 功能需求

## 7.1 工作流模板管理

### 功能点

- 新建、编辑、复制、停用、删除流程模板。
- 按社团、业务类型、状态、关键词筛选模板。
- 模板版本管理，已发起的实例继续使用发起时版本。
- 支持设置默认模板和业务绑定关系。
- 支持模板发布前校验，例如至少一个开始节点、至少一个结束路径、处理人规则合法。

### 模板字段建议

- `id`
- `clubId`
- `templateCode`
- `templateName`
- `businessType`
- `version`
- `status`
- `description`
- `starterRule`
- `createdBy`
- `createdAt`
- `updatedAt`

### 关键规则

- `templateCode + version` 唯一。
- 已发布模板不可直接修改流程结构，只能复制新版本。
- 停用模板不影响历史实例，但不能再发起新实例。

## 7.2 流程节点配置

### 功能点

- 配置节点名称、节点编码、节点类型、排序。
- 配置处理人规则：指定用户、指定角色、部门负责人、社团管理员、发起人上级、业务字段指定人。
- 配置会签/或签。
- 配置动作按钮：通过、驳回、退回补充、转交、加签、终止。
- 配置超时提醒和自动处理策略。
- 配置节点可见字段、可编辑字段、必填附件。

### 节点类型

- `start`：开始节点。
- `approval`：审批节点。
- `review`：审核/复核节点。
- `cc`：抄送节点。
- `condition`：条件节点。
- `end`：结束节点。

### 关键规则

- 首期可用“顺序节点 + 条件跳转”满足主要场景。
- 会签节点需支持全部通过才进入下一节点。
- 或签节点任一处理人通过即可流转。
- 退回补充后，申请人重新提交应回到退回前节点。

## 7.3 流程实例管理

### 功能点

- 创建流程实例。
- 查询流程详情。
- 查看流程图、当前节点、历史记录。
- 撤回流程。
- 管理员终止异常流程。
- 根据业务单据查询流程实例。

### 实例状态

- `draft`：草稿。
- `running`：流转中。
- `returned`：退回补充。
- `approved`：已通过。
- `rejected`：已驳回。
- `terminated`：已终止。
- `withdrawn`：已撤回。

### 关键规则

- 只有发起人可在首个审批人处理前撤回。
- 管理员终止流程必须填写原因。
- 业务模块状态以工作流回调为准，但业务模块需保留自身状态字段。

## 7.4 待办与审批中心

### 功能点

- 我的待办。
- 我的已办。
- 抄送我的。
- 我发起的。
- 按业务类型、状态、时间、关键词筛选。
- 批量审批。
- 审批意见快捷短语。
- 附件预览和下载。

### 列表字段建议

- 流程编号。
- 业务类型。
- 标题摘要。
- 发起人。
- 当前节点。
- 当前处理人。
- 状态。
- 发起时间。
- 更新时间。
- 超时状态。

## 7.5 工作流业务接入

### 首期接入场景

- 入会申请审核。
- 请假审批。
- 退社申请。
- 数据开放申请。
- 活动报名审核。
- 公文审批。

### 接入方式

- 业务模块负责业务单据的增删改查。
- 工作流模块负责流程实例、任务、流转和记录。
- 业务模块通过接口发起流程，并实现业务状态回调。
- 工作流模块只保存业务摘要，不复制完整业务正文。

### 业务类型编码建议

- `membership_application`
- `leave_application`
- `exit_application`
- `data_open_application`
- `activity_registration`
- `office_document`

## 7.6 知识库空间管理

### 功能点

- 新建知识库空间。
- 设置空间归属：全局、指定社团、实验室、机器人专用。
- 设置空间可见范围：公开、登录可见、社团成员可见、指定角色可见、仅管理员。
- 设置是否需要发布审核。
- 设置是否允许机器人引用。
- 设置是否参与 AI 检索。

### 空间字段建议

- `id`
- `clubId`
- `spaceCode`
- `spaceName`
- `visibility`
- `reviewRequired`
- `botEnabled`
- `aiSearchEnabled`
- `status`
- `createdBy`
- `createdAt`

## 7.7 知识分类与标签

### 功能点

- 树形分类管理。
- 标签管理。
- 分类排序。
- 分类权限继承。
- 分类下文章数量统计。

### 分类示例

- 社团制度。
- 招新指南。
- 活动手册。
- 请假与考勤。
- 实验室训练。
- 系统使用手册。
- 常见问题。
- API 与开放平台。

## 7.8 知识条目管理

### 功能点

- 新建、编辑、预览、发布、下线、删除。
- 支持 Markdown/富文本正文。
- 支持摘要、封面、附件、外链。
- 支持版本管理和版本回滚。
- 支持置顶、推荐、热门。
- 支持过期时间和复审提醒。
- 支持阅读量、收藏数、点赞/点踩、反馈。

### 条目字段建议

- `id`
- `spaceId`
- `categoryId`
- `title`
- `summary`
- `content`
- `contentFormat`
- `visibility`
- `status`
- `version`
- `tags`
- `sourceType`
- `sourceUrl`
- `expiredAt`
- `reviewedBy`
- `publishedAt`
- `createdBy`
- `updatedAt`

### 关键规则

- 已发布条目再次编辑时先生成草稿版本，不直接覆盖线上版本。
- 下线条目不参与门户展示、机器人问答和 AI 检索。
- 删除采用逻辑删除，保留审计记录。

## 7.9 FAQ 管理

### 功能点

- 单独维护问答型知识。
- 支持一个问题配置多个相似问法。
- 支持答案关联知识文章。
- 支持机器人优先命中 FAQ。
- 支持命中率、未解决率统计。

### FAQ 字段建议

- `id`
- `spaceId`
- `question`
- `similarQuestions`
- `answer`
- `relatedArticleIds`
- `status`
- `priority`
- `createdAt`
- `updatedAt`

## 7.10 知识检索

### 功能点

- 关键词搜索。
- 按空间、分类、标签、更新时间筛选。
- 标题、摘要、正文、附件文件名检索。
- 搜索结果高亮。
- 热门搜索词。
- 无结果反馈。
- 后续扩展向量检索和混合排序。

### 排序规则

1. 标题完全匹配。
2. FAQ 问法匹配。
3. 标签匹配。
4. 正文关键词匹配。
5. 热门和近期更新加权。

## 7.11 AI 知识问答

### 功能点

- 基于知识库检索结果生成回答。
- 回答必须附带引用来源。
- 支持回答“不知道”，避免无依据编造。
- 支持管理员配置可参与 AI 的空间。
- 支持记录问题、命中文档、回答、用户反馈。

### 首期策略

- 默认先做关键词检索和 FAQ 命中。
- AI 问答接口先预留，后续接入 LLM 和向量库。
- 若未配置 AI，则机器人返回检索结果列表和摘要。

### 安全规则

- AI 只能使用当前用户有权限访问的知识片段。
- 私密空间不得被公开群聊机器人引用。
- 回答中不得泄露后台 token、API key、身份证号等敏感字段。

## 7.12 机器人接入

### 功能点

- 群聊绑定知识库空间。
- 配置触发方式，例如 `问 xxx`、`/kb xxx`、关键词自动回复。
- 支持返回 FAQ 答案、文章摘要、文章链接。
- 支持未命中时记录问题，供编辑后续补充知识。
- 支持机器人回答频率限制。

### 关键规则

- QQ 群只能访问绑定空间中允许机器人引用的内容。
- 管理员可为不同群绑定不同知识库空间。
- 私聊查询可根据用户绑定身份返回更高权限内容。

## 7.13 通知与提醒

### 工作流通知场景

- 流程发起成功。
- 新待办任务。
- 审批通过。
- 审批驳回。
- 退回补充。
- 流程终止。
- 超时提醒。
- 抄送通知。

### 知识库通知场景

- 文章提交审核。
- 文章审核通过/驳回。
- 文章即将过期。
- 用户反馈待处理。
- 高频未命中问题待补充。

### 通知渠道

- 站内信。
- QQ 机器人。
- 邮件或短信预留。

## 7.14 权限与审计

### 功能点

- 工作流模板管理权限。
- 流程实例查看权限。
- 审批任务处理权限。
- 知识空间管理权限。
- 知识文章编辑/发布/下线权限。
- 操作日志与审计追踪。

### 关键规则

- 用户只能处理分配给自己的任务。
- 管理员可查看本社团流程，系统管理员可查看全局流程。
- 知识库权限按空间、分类、文章三级控制，文章权限可覆盖分类默认权限。
- 所有审批动作、知识发布动作必须记录操作人、时间、IP、User-Agent。

## 8. 页面需求

## 8.1 后台工作台

- 待办数量。
- 超时任务数量。
- 我发起的流程状态统计。
- 最近知识反馈。
- 高频未命中问题。

## 8.2 工作流模板页

- 模板列表。
- 模板新增/编辑抽屉。
- 节点配置表格。
- 处理人规则配置。
- 通知模板配置。
- 模板版本记录。

## 8.3 流程中心页

- 我的待办。
- 我的已办。
- 我发起的。
- 抄送我的。
- 流程详情弹窗或详情页。
- 审批操作面板。
- 流转记录时间线。

## 8.4 知识库管理页

- 左侧空间与分类树。
- 中间文章列表。
- 右侧筛选和批量操作。
- 文章编辑器。
- 发布审核记录。
- 版本历史。

## 8.5 知识库门户页

- 搜索框。
- 分类导航。
- 热门文章。
- FAQ 区域。
- 文章详情。
- 反馈入口。

## 8.6 机器人配置页

- 群列表。
- 绑定知识库空间。
- 开启/关闭自动回复。
- 触发词配置。
- 回答频率限制。
- 最近问答日志。

## 9. 接口需求草案

### 9.1 工作流接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/workflow/templates` | 查询流程模板 |
| `POST` | `/api/v1/workflow/templates` | 新建流程模板 |
| `PUT` | `/api/v1/workflow/templates/{id}` | 更新流程模板草稿 |
| `POST` | `/api/v1/workflow/templates/{id}/publish` | 发布流程模板 |
| `POST` | `/api/v1/workflow/instances` | 发起流程 |
| `GET` | `/api/v1/workflow/instances/{id}` | 查看流程详情 |
| `POST` | `/api/v1/workflow/instances/{id}/withdraw` | 撤回流程 |
| `POST` | `/api/v1/workflow/tasks/{taskId}/actions` | 处理审批任务 |
| `GET` | `/api/v1/workflow/tasks/todo` | 我的待办 |
| `GET` | `/api/v1/workflow/tasks/done` | 我的已办 |

### 9.2 知识库接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/kb/spaces` | 查询知识库空间 |
| `POST` | `/api/v1/kb/spaces` | 新建知识库空间 |
| `GET` | `/api/v1/kb/categories` | 查询分类树 |
| `POST` | `/api/v1/kb/articles` | 新建知识文章 |
| `PUT` | `/api/v1/kb/articles/{id}` | 更新知识文章 |
| `POST` | `/api/v1/kb/articles/{id}/publish` | 发布知识文章 |
| `POST` | `/api/v1/kb/articles/{id}/offline` | 下线知识文章 |
| `GET` | `/api/v1/kb/search` | 知识检索 |
| `POST` | `/api/v1/kb/faqs` | 新建 FAQ |
| `POST` | `/api/v1/kb/bot/query` | 机器人知识查询 |

### 9.3 工作流发起请求示例

```json
{
  "templateCode": "leave_application_default",
  "businessType": "leave_application",
  "businessId": 10001,
  "title": "张三的请假申请",
  "starterUserId": 12,
  "summary": {
    "startTime": "2026-06-16 09:00:00",
    "endTime": "2026-06-16 18:00:00",
    "reason": "参加校级比赛"
  }
}
```

### 9.4 审批动作请求示例

```json
{
  "action": "approve",
  "comment": "同意，请注意按时销假。",
  "nextAssigneeIds": [18],
  "attachments": []
}
```

### 9.5 机器人知识查询请求示例

```json
{
  "groupId": "123456789",
  "qqOpenid": "user-openid",
  "question": "请假怎么申请？",
  "channel": "qq_group"
}
```

## 10. 数据库表建议

### 10.1 流程模板表 `workflow_template`

```sql
CREATE TABLE `workflow_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL COMMENT '所属社团，为空表示全局模板',
  `template_code` VARCHAR(64) NOT NULL,
  `template_name` VARCHAR(128) NOT NULL,
  `business_type` VARCHAR(64) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
  `starter_rule` JSON DEFAULT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_template_code_version` (`template_code`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.2 流程节点表 `workflow_node`

```sql
CREATE TABLE `workflow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `node_code` VARCHAR(64) NOT NULL,
  `node_name` VARCHAR(128) NOT NULL,
  `node_type` VARCHAR(32) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `assignee_rule` JSON DEFAULT NULL,
  `action_config` JSON DEFAULT NULL,
  `timeout_config` JSON DEFAULT NULL,
  `field_permission` JSON DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_node_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.3 流程实例表 `workflow_instance`

```sql
CREATE TABLE `workflow_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `template_code` VARCHAR(64) NOT NULL,
  `business_type` VARCHAR(64) NOT NULL,
  `business_id` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `summary` JSON DEFAULT NULL,
  `starter_user_id` INT NOT NULL,
  `current_node_code` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'running',
  `started_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `finished_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_instance_business` (`business_type`, `business_id`),
  KEY `idx_workflow_instance_starter` (`starter_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.4 流程任务表 `workflow_task`

```sql
CREATE TABLE `workflow_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `instance_id` BIGINT NOT NULL,
  `node_code` VARCHAR(64) NOT NULL,
  `node_name` VARCHAR(128) NOT NULL,
  `assignee_user_id` INT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending',
  `action` VARCHAR(32) DEFAULT NULL,
  `comment` VARCHAR(1000) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `handled_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_workflow_task_assignee` (`assignee_user_id`, `status`),
  KEY `idx_workflow_task_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.5 知识库空间表 `kb_space`

```sql
CREATE TABLE `kb_space` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL,
  `space_code` VARCHAR(64) NOT NULL,
  `space_name` VARCHAR(128) NOT NULL,
  `visibility` VARCHAR(32) NOT NULL DEFAULT 'login',
  `review_required` TINYINT(1) NOT NULL DEFAULT 0,
  `bot_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `ai_search_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL DEFAULT 'enabled',
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_space_code` (`space_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.6 知识文章表 `kb_article`

```sql
CREATE TABLE `kb_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `space_id` BIGINT NOT NULL,
  `category_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(255) NOT NULL,
  `summary` VARCHAR(512) DEFAULT NULL,
  `content` LONGTEXT NOT NULL,
  `content_format` VARCHAR(32) NOT NULL DEFAULT 'markdown',
  `visibility` VARCHAR(32) NOT NULL DEFAULT 'inherit',
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
  `version` INT NOT NULL DEFAULT 1,
  `tags` JSON DEFAULT NULL,
  `expired_at` DATETIME DEFAULT NULL,
  `published_at` DATETIME DEFAULT NULL,
  `created_by` INT DEFAULT NULL,
  `updated_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kb_article_space_status` (`space_id`, `status`),
  FULLTEXT KEY `ft_kb_article_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.7 FAQ 表 `kb_faq`

```sql
CREATE TABLE `kb_faq` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `space_id` BIGINT NOT NULL,
  `question` VARCHAR(255) NOT NULL,
  `similar_questions` JSON DEFAULT NULL,
  `answer` TEXT NOT NULL,
  `related_article_ids` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'enabled',
  `priority` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kb_faq_space_status` (`space_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.8 机器人知识绑定表 `kb_bot_binding`

```sql
CREATE TABLE `kb_bot_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` VARCHAR(64) NOT NULL,
  `space_id` BIGINT NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `trigger_config` JSON DEFAULT NULL,
  `rate_limit_config` JSON DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_bot_group_space` (`group_id`, `space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 11. 非功能需求

### 11.1 安全性

- 所有接口必须接入 Sa-Token 鉴权。
- 审批任务处理必须校验任务归属。
- 知识库检索必须先做权限过滤。
- 机器人接口必须校验内部 token 或签名。
- 敏感字段不得进入机器人公开回答。

### 11.2 性能

- 待办列表在 10 万级任务量下分页查询响应不超过 1 秒。
- 知识库关键词检索在 1 万篇文章内响应不超过 1 秒。
- AI 向量化、附件解析等耗时任务必须异步执行。
- 高频机器人查询需要缓存 FAQ 和热门文章。

### 11.3 可用性

- 工作流核心流转需使用事务保证任务状态与实例状态一致。
- 业务回调失败需进入补偿队列，并允许管理员手动重试。
- 知识索引生成失败不得影响文章发布，但需显示索引异常状态。

### 11.4 可审计

- 流程模板发布、审批动作、流程终止必须记录审计日志。
- 知识文章发布、下线、删除、权限变更必须记录审计日志。
- 机器人问答需记录问题、命中内容、回答渠道和反馈结果。

### 11.5 兼容性

- 现有入会、请假、数据开放等模块可先保留原逻辑。
- 新工作流模块通过适配层逐步接管审批能力。
- 迁移期间允许同一业务类型配置“旧审批模式”或“工作流模式”。

## 12. 迭代计划

### 12.1 MVP 阶段

- 新增工作流基础表。
- 实现顺序审批流。
- 实现待办、已办、我发起的。
- 接入请假审批或数据开放申请作为首个试点。
- 新增知识库空间、分类、文章、FAQ。
- 实现后台知识库管理和门户检索。
- 实现机器人知识查询接口。

### 12.2 V1.1 阶段

- 接入入会、退社、活动报名、公文审批。
- 增加会签、或签、退回补充、转交。
- 增加知识发布审核流程。
- 增加知识反馈和未命中问题池。
- 增加机器人群绑定配置页。

### 12.3 V1.2 阶段

- 接入向量检索。
- 接入 AI 知识问答。
- 增加流程统计和知识库质量看板。
- 增加流程模板复制、版本对比和可视化预览。

## 13. 验收标准

- 管理员可以创建并发布一个两级审批流程模板。
- 用户可以基于模板发起流程，审批人能在待办中心完成处理。
- 流程完成后业务模块能收到状态回调并更新业务状态。
- 用户可以在后台创建知识库空间、分类、文章和 FAQ。
- 门户可以搜索已发布且有权限访问的知识内容。
- QQ 机器人可以调用知识查询接口并返回 FAQ 或文章摘要。
- 所有关键操作都有审计记录。
- 停用模板不影响历史流程继续查看。

## 14. 风险与待确认问题

- 是否需要首期支持可视化流程画布，还是先用表单式节点配置。
- 知识库正文编辑器采用 Markdown、富文本，还是双模式。
- AI 问答使用哪一家模型服务，以及是否允许外部模型处理内部知识。
- 机器人公开群聊能访问哪些知识空间，需要明确默认安全策略。
- 现有入会审批与请假审批迁移优先级需要确认。
- 是否需要附件正文解析，例如 PDF、Word、Excel 内容入库检索。
