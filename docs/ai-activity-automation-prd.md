# AI 活动自动化系统 PRD

## 1. 文档信息

| 项目名称 | AI 活动自动化系统 | 版本号 | V1.0.0 |
| --- | --- | --- | --- |
| 依赖系统 | OpenAtom 社团管理系统、工作流、知识库、公文模块 | 输出日期 | 2026-06-16 |
| AI 服务 | DeepSeek API | 文档状态 | 需求草案 |

## 2. 项目背景

当前 OpenAtom System 已具备活动管理、活动报名、公文导出、通知、审批等基础能力，但活动筹备仍高度依赖人工：

- 用户需要先自己整理活动想法，再手工扩写成活动策划案。
- 活动策划案需要多次沟通修改，缺少结构化确认流程。
- 策划案确认后，还需要手工填写活动申请书、活动申请表、志愿者申请表等 Word 文件。
- 当前公文模块主要由后端代码固定生成文档，不能灵活复用学校或社团提前上传的 Word 模板。

因此需要接入 DeepSeek，建设“AI 活动自动化”能力，让用户只输入一个初步需求，系统自动进行需求澄清、生成建议、多轮确认、生成活动策划案，并在策划案确认后根据预上传 Word 模板自动生成正式 docx 材料。

## 3. 产品目标

- 接入 DeepSeek API，提供稳定、可审计、可配置的 AI 生成能力。
- 支持用户输入活动初步需求后，AI 自动给出活动建议和补充问题。
- 支持多轮对话澄清活动目标、对象、时间、地点、预算、人员、风险等信息。
- 在用户确认后，AI 自动生成结构完整、可编辑的活动策划案。
- 策划案确认后，系统基于预上传的 Word 模板生成 docx 文件。
- 首期生成三类材料：活动申请书、活动申请表、志愿者申请表。
- 生成结果进入现有活动、公文、工作流体系，便于后续审批、发布和归档。

## 4. 用户角色

### 4.1 活动发起人

- 输入活动需求。
- 与 AI 多轮对话，补充活动信息。
- 确认 AI 生成的活动建议和策划案。
- 下载或提交 AI 生成的 docx 材料。

### 4.2 社团管理员/活动负责人

- 审核活动策划案。
- 管理活动文档模板。
- 调整 AI 生成内容。
- 将活动发布到门户或小程序。

### 4.3 模板管理员

- 上传学校或社团提供的 Word 模板。
- 配置模板类型、模板变量、示例值和适用范围。
- 维护模板版本。

### 4.4 系统管理员

- 配置 DeepSeek API Key、模型、限流、超时和内容安全规则。
- 查看 AI 调用日志、失败记录、成本统计。
- 管理全局提示词和输出格式。

## 5. 业务范围

### 5.1 本期范围

- DeepSeek API 接入。
- AI 活动需求多轮澄清。
- AI 活动建议生成。
- AI 活动策划案生成。
- 策划案人工确认与编辑。
- Word 模板上传与变量配置。
- 基于模板生成 docx。
- 生成活动申请书、活动申请表、志愿者申请表。
- 生成记录、AI 调用记录和文档下载。

### 5.2 暂不纳入本期

- AI 自动提交学校外部系统。
- AI 自动盖章或电子签章。
- 复杂流程画布。
- 多模型自动路由。
- 图片海报自动生成。
- 预算报销自动化。

### 5.3 后续扩展

- 自动生成活动宣传文案、推文、报名表字段和签到方案。
- 根据知识库中的历史活动自动复用优秀策划案结构。
- 自动生成风险预案、主持稿、物料清单、人员分工表。
- 接入工作流后自动发起活动审批。
- 自动创建活动并发布到门户、小程序和 QQ 群。

## 6. 核心业务流程

### 6.1 AI 活动自动化主流程

1. 用户进入“AI 活动自动化”页面。
2. 用户输入一句活动需求，例如：“我想办一个面向新生的开源社团破冰活动”。
3. 系统创建 AI 活动草稿会话。
4. DeepSeek 根据需求生成初步活动建议，并提出需要补充的问题。
5. 用户与 AI 多轮对话，补齐活动目标、活动对象、时间、地点、人数、预算、人员分工、报名方式、志愿者需求等信息。
6. AI 判断信息足够后，给出“需求确认摘要”。
7. 用户点击“确认需求”。
8. DeepSeek 生成完整活动策划案。
9. 用户在线编辑或要求 AI 修改策划案。
10. 用户点击“确认策划案”。
11. 系统根据策划案提取模板变量。
12. 用户选择或系统自动匹配 Word 模板。
13. 系统生成活动申请书、活动申请表、志愿者申请表 docx。
14. 用户下载文档，或提交到工作流进行审批。

### 6.2 多轮澄清流程

AI 每轮回复应包含：

- 当前理解：总结用户已经提供的信息。
- 建议方向：给出活动定位、形式或优化建议。
- 待确认问题：最多 3-5 个关键问题。
- 可选方案：对不确定问题给出可选项，方便用户快速选择。

当以下信息基本完整时，AI 可进入策划案生成阶段：

- 活动名称或主题方向。
- 活动目标。
- 活动对象。
- 活动时间或时间范围。
- 活动地点或地点需求。
- 预计人数。
- 活动形式和主要环节。
- 主办/承办单位。
- 负责人和联系方式。
- 志愿者或工作人员需求。
- 经费预算或是否无预算。
- 安全风险和应急要求。

### 6.3 策划案确认流程

1. AI 生成策划案草稿。
2. 系统以结构化章节展示。
3. 用户可选择“继续修改”“局部重写”“确认策划案”。
4. 若用户要求修改，系统将修改要求和当前策划案继续发送给 DeepSeek。
5. 每次修改都保存一个策划案版本。
6. 用户确认后，策划案状态变为 `confirmed`。
7. 已确认策划案可进入文档生成和活动创建。

### 6.4 Word 模板生成流程

1. 模板管理员提前上传学校或社团的 `.docx` 模板。
2. 系统解析模板中的占位符，例如 `{{activityName}}`、`{{activityDate}}`、`{{organizer}}`。
3. 管理员为模板变量配置中文名称、是否必填、默认值、数据来源。
4. 用户确认策划案后，系统从策划案中抽取结构化字段。
5. 系统将字段映射到模板变量。
6. 若存在缺失必填变量，页面要求用户补充。
7. 系统替换模板变量并生成新的 docx。
8. 生成后的 docx 与活动草稿、策划案版本关联保存。

## 7. 功能需求

## 7.1 DeepSeek 配置管理

### 功能点

- 配置 DeepSeek API Base URL。
- 配置 API Key，后端加密保存，不允许前端明文展示。
- 配置模型名称，例如 `deepseek-chat`。
- 配置默认 temperature、max tokens、超时时间。
- 配置每日调用额度、单用户调用额度。
- 配置失败重试次数。
- 支持启用/停用 AI 功能。

### 关键规则

- API Key 必须通过环境变量或加密配置注入。
- 前端不得直接调用 DeepSeek，统一由后端代理。
- 所有 AI 调用必须记录请求摘要、响应摘要、耗时、状态和操作人。

## 7.2 AI 活动会话

### 功能点

- 创建活动自动化会话。
- 保存多轮对话上下文。
- 支持继续对话、重命名、归档、删除。
- 支持从历史会话恢复。
- 支持一键生成需求确认摘要。

### 会话状态

- `drafting`：需求澄清中。
- `requirement_confirmed`：需求已确认。
- `plan_generated`：策划案已生成。
- `plan_confirmed`：策划案已确认。
- `documents_generated`：文档已生成。
- `submitted`：已提交审批。
- `archived`：已归档。

## 7.3 活动建议生成

### 功能点

- 根据用户输入的初始需求生成活动建议。
- 给出活动主题建议、活动形式建议、适合人群、亮点设计。
- 给出风险提醒和需补充信息。
- 支持用户选择某个建议继续深化。

### 输出格式要求

AI 输出需结构化为：

- `summary`：当前理解。
- `suggestions`：建议列表。
- `questions`：待补充问题。
- `options`：可选方案。
- `missingFields`：缺失字段。

## 7.4 活动策划案生成

### 策划案章节要求

- 活动名称。
- 活动背景。
- 活动目的与意义。
- 活动主题。
- 活动时间。
- 活动地点。
- 活动对象。
- 主办单位/承办单位。
- 活动流程。
- 人员分工。
- 宣传方案。
- 报名方式。
- 志愿者需求。
- 物资清单。
- 经费预算。
- 风险预案。
- 应急处理。
- 预期效果。
- 附件说明。

### 功能点

- 一键生成完整策划案。
- 支持章节级重新生成。
- 支持用户手动编辑。
- 支持保存版本。
- 支持导出 Markdown。
- 支持将策划案转为活动草稿。

### 关键规则

- 策划案必须以事实和用户确认信息为准。
- 对缺失但必须出现的信息，应标记为“待补充”，不得凭空编造具体姓名、电话、经费金额。
- 生成内容应适合高校社团活动场景，语言正式、清晰、可提交。

## 7.5 策划案结构化抽取

### 功能点

- 从策划案中抽取活动字段。
- 支持用户校对字段。
- 字段可用于活动创建和 Word 模板填充。

### 字段建议

- `activityName`
- `activityTheme`
- `activityBackground`
- `activityPurpose`
- `activityDate`
- `startTime`
- `endTime`
- `location`
- `targetAudience`
- `expectedParticipants`
- `organizer`
- `coOrganizer`
- `advisorName`
- `principalName`
- `principalPhone`
- `volunteerCount`
- `volunteerRequirements`
- `budgetTotal`
- `budgetDetails`
- `safetyPlan`
- `emergencyPlan`

## 7.6 Word 模板管理

### 功能点

- 上传 `.docx` 模板。
- 设置模板名称、模板类型、适用社团、状态。
- 解析模板占位符。
- 配置变量含义、必填规则、默认值。
- 预览模板变量。
- 启用/停用模板。
- 模板版本管理。

### 模板类型

- `activity_proposal`：活动申请书。
- `activity_application_form`：活动申请表。
- `volunteer_application_form`：志愿者申请表。

### 占位符规范

模板中使用双花括号占位：

```text
{{activityName}}
{{activityDate}}
{{location}}
{{organizer}}
{{principalName}}
{{principalPhone}}
{{volunteerCount}}
```

### 关键规则

- 首期只支持 `.docx` 模板。
- 若用户上传 `.doc`，系统提示先转换为 `.docx` 后再上传。
- 模板文件需保存原始文件和版本号。
- 模板变量解析失败时不允许启用模板。

## 7.7 docx 文档生成

### 功能点

- 基于确认后的策划案生成 docx。
- 支持一次生成三份文档。
- 支持生成前变量检查。
- 支持用户补充缺失字段。
- 支持下载单个文件或打包下载。
- 支持重新生成。
- 支持生成记录查询。

### 生成文档

- 活动申请书。
- 活动申请表。
- 志愿者申请表。

### 关键规则

- 生成文档必须保留原模板样式、表格、页眉页脚和基础排版。
- 未匹配变量需在生成前明确提示，不生成带裸占位符的正式文档。
- 生成文件名建议：`活动名称-活动申请书-生成时间.docx`。

## 7.8 活动草稿创建

### 功能点

- 从确认后的策划案一键创建活动草稿。
- 自动填充活动标题、摘要、详情、时间、地点、报名字段。
- 支持管理员二次编辑。
- 支持发布到门户。

### 与现有活动模块映射

- `ClubActivity.title` 对应活动名称。
- `ClubActivity.summary` 对应活动摘要。
- `ClubActivity.descriptionMarkdown` 对应策划案中的活动介绍、流程和说明。
- `ClubActivity.activityAt` 对应活动开始时间。
- `ClubActivity.endAt` 对应活动结束时间。
- `ClubActivity.location` 对应活动地点。
- `ClubActivity.registrationFields` 可由 AI 建议生成。

## 7.9 审批与工作流接入

### 功能点

- 策划案确认后可提交“活动立项审批”。
- 三份 docx 材料作为附件进入审批流程。
- 审批通过后，活动可发布。
- 审批驳回后，可回到 AI 修改策划案。

### 工作流业务类型

- `ai_activity_plan`
- `activity_document_generation`
- `activity_approval`

## 7.10 AI 调用日志

### 功能点

- 记录调用场景。
- 记录模型名称。
- 记录 prompt 版本。
- 记录 token 用量或估算字符数。
- 记录响应时间。
- 记录成功/失败状态。
- 支持管理员查看失败原因。

### 隐私规则

- 日志中不保存 API Key。
- 涉及手机号、身份证、学号等敏感字段时应脱敏展示。
- 用户可查看自己会话中的 AI 内容，但不能查看他人会话。

## 8. 页面需求

## 8.1 AI 活动自动化页面

- 左侧展示会话列表。
- 中间展示多轮对话。
- 右侧展示活动信息补全状态。
- 底部输入需求和修改意见。
- 支持“确认需求”“生成策划案”“确认策划案”“生成文档”主操作。

## 8.2 策划案编辑页面

- 章节式策划案编辑。
- 支持章节重写。
- 支持版本切换。
- 支持字段抽取预览。
- 支持确认策划案。

## 8.3 模板管理页面

- 模板列表。
- 上传模板。
- 变量解析结果。
- 变量配置表。
- 模板版本记录。
- 模板启用/停用。

## 8.4 文档生成页面

- 展示待生成文档清单。
- 展示模板匹配状态。
- 展示变量完整度。
- 支持补充缺失字段。
- 支持生成、预览、下载、重新生成。

## 8.5 AI 配置页面

- DeepSeek API 配置。
- 模型参数配置。
- 调用额度配置。
- Prompt 模板配置。
- 调用日志入口。

## 9. 接口需求草案

### 9.1 AI 活动会话接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/ai/activity/sessions` | 创建活动 AI 会话 |
| `GET` | `/api/v1/ai/activity/sessions` | 查询我的活动 AI 会话 |
| `GET` | `/api/v1/ai/activity/sessions/{sessionId}` | 查看会话详情 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/messages` | 发送一轮对话 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/confirm-requirement` | 确认需求 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/generate-plan` | 生成策划案 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/revise-plan` | 修改策划案 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/confirm-plan` | 确认策划案 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/create-activity` | 创建活动草稿 |

### 9.2 模板与文档接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/document-templates` | 上传 docx 模板 |
| `GET` | `/api/v1/document-templates` | 查询模板列表 |
| `GET` | `/api/v1/document-templates/{templateId}/variables` | 查询模板变量 |
| `PUT` | `/api/v1/document-templates/{templateId}/variables` | 保存变量配置 |
| `POST` | `/api/v1/ai/activity/sessions/{sessionId}/documents/generate` | 生成活动文档 |
| `GET` | `/api/v1/ai/activity/sessions/{sessionId}/documents` | 查询生成文档 |
| `GET` | `/api/v1/generated-documents/{documentId}/download` | 下载生成文档 |

### 9.3 AI 配置接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/ai/settings` | 查询 AI 配置 |
| `PUT` | `/api/v1/ai/settings` | 更新 AI 配置 |
| `GET` | `/api/v1/ai/logs` | 查询 AI 调用日志 |

### 9.4 发送对话请求示例

```json
{
  "message": "我想办一个面向新生的开源社团破冰活动，最好轻松一点，可以让大家认识社团项目。",
  "mode": "requirement_clarification"
}
```

### 9.5 AI 结构化响应示例

```json
{
  "summary": "你希望举办一场面向新生的开源社团破冰活动，重点是轻松交流和项目认知。",
  "suggestions": [
    "采用项目体验摊位 + 组队小游戏 + 社团路线介绍的组合形式。",
    "活动时长建议控制在 90-120 分钟，降低新生参与门槛。"
  ],
  "questions": [
    "预计参与人数是多少？",
    "活动地点是室内教室、报告厅还是户外空间？",
    "是否需要招募志愿者协助签到、引导和拍摄？"
  ],
  "missingFields": ["activityDate", "location", "expectedParticipants", "volunteerCount"]
}
```

## 10. 数据库表建议

### 10.1 AI 活动会话表 `ai_activity_session`

```sql
CREATE TABLE `ai_activity_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL,
  `user_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'drafting',
  `requirement_summary` JSON DEFAULT NULL,
  `confirmed_plan_id` BIGINT DEFAULT NULL,
  `activity_id` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_session_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.2 AI 对话消息表 `ai_activity_message`

```sql
CREATE TABLE `ai_activity_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `role` VARCHAR(32) NOT NULL COMMENT 'user/assistant/system',
  `content` MEDIUMTEXT NOT NULL,
  `structured_payload` JSON DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_message_session` (`session_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.3 活动策划案表 `ai_activity_plan`

```sql
CREATE TABLE `ai_activity_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `title` VARCHAR(255) NOT NULL,
  `content_markdown` MEDIUMTEXT NOT NULL,
  `structured_fields` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_activity_plan_session` (`session_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.4 文档模板表 `document_template`

```sql
CREATE TABLE `document_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `club_id` INT DEFAULT NULL,
  `template_type` VARCHAR(64) NOT NULL,
  `template_name` VARCHAR(128) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `file_url` VARCHAR(512) NOT NULL,
  `variables` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'enabled',
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_document_template_type` (`template_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.5 生成文档表 `generated_document`

```sql
CREATE TABLE `generated_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `template_id` BIGINT NOT NULL,
  `document_type` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_url` VARCHAR(512) NOT NULL,
  `filled_variables` JSON DEFAULT NULL,
  `created_by` INT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_generated_document_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.6 AI 调用日志表 `ai_call_log`

```sql
CREATE TABLE `ai_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `scene` VARCHAR(64) NOT NULL,
  `provider` VARCHAR(32) NOT NULL DEFAULT 'deepseek',
  `model` VARCHAR(64) NOT NULL,
  `prompt_version` VARCHAR(64) DEFAULT NULL,
  `request_summary` VARCHAR(1000) DEFAULT NULL,
  `response_summary` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `error_message` VARCHAR(1000) DEFAULT NULL,
  `duration_ms` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_call_log_scene` (`scene`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 11. DeepSeek Prompt 设计

### 11.1 需求澄清 Prompt 要求

- 角色：高校社团活动策划顾问。
- 目标：帮助用户把模糊活动想法变成可执行活动需求。
- 限制：每轮最多问 5 个问题，优先问影响审批和落地的信息。
- 输出：必须返回 JSON，包含 summary、suggestions、questions、missingFields。

### 11.2 策划案生成 Prompt 要求

- 角色：高校社团活动策划案撰写助手。
- 目标：生成正式、完整、可提交审批的活动策划案。
- 输入：已确认需求摘要、多轮对话、学校/社团要求、历史模板要求。
- 限制：不得编造联系人、电话、预算金额；缺失内容标为待补充。
- 输出：Markdown 策划案 + 结构化字段 JSON。

### 11.3 文档字段抽取 Prompt 要求

- 角色：活动文档字段抽取助手。
- 目标：从策划案中抽取模板变量需要的字段。
- 输出：严格 JSON。
- 缺失字段：返回 `null`，并列入 `missingFields`。

## 12. 技术实现建议

### 12.1 后端模块建议

- `AiProviderService`：封装 DeepSeek API 调用。
- `AiActivityService`：管理活动 AI 会话、对话、策划案。
- `DocumentTemplateService`：管理 docx 模板和变量。
- `DocumentGenerationService`：负责模板填充和 docx 生成。
- `AiCallLogService`：记录调用日志。

### 12.2 docx 生成建议

- 当前 `OfficeDocumentServiceImpl` 使用 Apache POI 代码式生成文档。
- 新模板生成能力建议使用 `.docx` 模板占位符替换。
- 后端仍可基于 Apache POI 实现，重点支持段落、表格、页眉页脚中的占位符。
- 若后续模板复杂，可引入 poi-tl 等模板引擎，但首期应先评估依赖体积和兼容性。

### 12.3 AI 调用安全建议

- 使用后端服务端代理 DeepSeek。
- API Key 放在环境变量，例如 `DEEPSEEK_API_KEY`。
- 请求前做敏感信息最小化。
- 响应后做 JSON Schema 校验。
- AI 输出进入业务前必须允许用户确认。

## 13. 非功能需求

### 13.1 稳定性

- AI 调用失败时，页面应提示可重试。
- DeepSeek 超时不能影响其他活动管理功能。
- 策划案和文档生成过程应保存中间状态，避免刷新丢失。

### 13.2 性能

- 普通对话接口响应建议控制在 30 秒内。
- 文档生成接口响应建议控制在 10 秒内。
- AI 生成策划案可采用异步任务或流式输出。

### 13.3 可追溯

- 每次 AI 生成建议、策划案、字段抽取都需要记录版本。
- 用户确认动作需要记录时间和操作人。
- 生成的 docx 需关联模板版本和策划案版本。

### 13.4 内容安全

- AI 生成内容不得包含违法违规、歧视、危险活动指导等内容。
- 活动安全预案必须由用户或管理员确认。
- 涉及校内审批的正式材料必须人工确认后才允许下载或提交。

## 14. 迭代计划

### 14.1 MVP 阶段

- 接入 DeepSeek。
- 实现活动 AI 多轮对话。
- 实现活动建议和策划案生成。
- 实现策划案确认。
- 实现上传三类 docx 模板。
- 实现模板变量替换生成 docx。

### 14.2 V1.1 阶段

- 一键创建活动草稿。
- 自动生成报名字段。
- 接入工作流审批。
- 支持打包下载三份文档。
- 增加 AI 调用日志后台。

### 14.3 V1.2 阶段

- 接入知识库历史活动案例。
- AI 自动生成宣传文案和 QQ 群通知。
- AI 自动生成物资清单、预算表、人员排班表。
- 支持流式响应和更好的章节级编辑。

## 15. 验收标准

- 用户输入一句活动需求后，AI 能返回活动建议和待补充问题。
- 用户完成多轮对话后，可以确认需求摘要。
- AI 能基于确认需求生成完整活动策划案。
- 用户可以编辑并确认策划案。
- 管理员可以上传三类 docx 模板并解析变量。
- 系统能基于确认策划案生成活动申请书、活动申请表、志愿者申请表。
- 生成 docx 不出现未替换占位符。
- AI 调用日志可在后台查询。
- DeepSeek API Key 不会出现在前端或日志中。

## 16. 待确认问题

- 你现有的申请书、申请表、志愿者申请表模板是 `.doc` 还是 `.docx`。
- 模板里的字段是否可以改成 `{{变量名}}` 格式。
- 三份文档是否都来自同一份策划案字段，还是每份有额外字段。
- 活动是否需要自动进入审批流程，还是先只生成文档供下载。
- 策划案是否需要套用固定学校格式。
- DeepSeek 使用官方 API 还是兼容 OpenAI 格式的代理地址。

