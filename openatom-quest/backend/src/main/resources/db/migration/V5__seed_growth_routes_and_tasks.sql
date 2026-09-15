INSERT INTO quest_member (
    nickname, email, status, current_level, total_points, bio
)
SELECT 'Quest 内容组', 'quest-content@system.local', 'DISABLED', 'L0', 0, '系统内置成长路线与任务的内容归属账号'
WHERE NOT EXISTS (
    SELECT 1 FROM quest_member WHERE email = 'quest-content@system.local'
);

SET @quest_seed_owner = COALESCE(
    (
        SELECT member.id
        FROM quest_member member
        JOIN quest_member_role member_role ON member_role.member_id = member.id
        JOIN quest_role role ON role.id = member_role.role_id
        WHERE role.role_key = 'ADMIN' AND member.status = 'ACTIVE'
        ORDER BY member.id
        LIMIT 1
    ),
    (SELECT id FROM quest_member WHERE email = 'quest-content@system.local' ORDER BY id LIMIT 1)
);

CREATE TEMPORARY TABLE quest_seed_route (
    direction_key VARCHAR(64) NOT NULL,
    route_key VARCHAR(64) NOT NULL,
    route_name VARCHAR(128) NOT NULL,
    route_description VARCHAR(1000) NOT NULL,
    foundation_topic VARCHAR(500) NOT NULL,
    practice_artifact VARCHAR(500) NOT NULL,
    contribution_target VARCHAR(500) NOT NULL,
    reference_url VARCHAR(512) NOT NULL,
    PRIMARY KEY (direction_key)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO quest_seed_route VALUES
    ('openharmony', 'openharmony-growth', 'OpenHarmony 应用开发路线',
     '从 ArkTS 与 ArkUI 基础出发，完成可运行应用并参与 OpenHarmony 相关项目协作。',
     'ArkTS 基础语法、ArkUI 声明式界面、页面路由和应用生命周期',
     '一个包含列表、详情、表单校验和本地持久化的 OpenHarmony 应用',
     'OpenHarmony 官方示例、社区文档或社团鸿蒙项目中的真实 Issue',
     'https://docs.openharmony.cn/'),
    ('frontend', 'frontend-growth', '现代前端开发路线',
     '掌握现代 Web 基础、Vue 工程化和前端质量要求，逐步参与社团前端项目。',
     '语义化 HTML、响应式 CSS、JavaScript、TypeScript 与 Vue 组件开发',
     '一个适配手机和桌面的 Vue 数据看板，包含请求、加载、空态和错误态',
     '社团官网、活动系统或 OpenAtom Quest 前端中的真实 Issue',
     'https://developer.mozilla.org/zh-CN/docs/Learn_web_development'),
    ('fullstack', 'fullstack-growth', '全栈应用开发路线',
     '打通浏览器、服务端、数据库和部署流程，独立交付一个可验证的完整功能。',
     'Vue、TypeScript、Spring Boot、REST API、MySQL 与前后端联调',
     '一个具有增删改查、校验、异常反馈和数据库持久化的全栈业务模块',
     '社团业务系统中的一项端到端功能或跨前后端缺陷',
     'https://spring.io/guides/'),
    ('java-backend', 'java-backend-growth', 'Java 后端工程路线',
     '从 Java 与 Spring Boot 基础进入接口设计、数据持久化、测试和真实后端贡献。',
     'Java 核心语法、Spring Boot、RESTful API、MyBatis 与 MySQL',
     '一个带分页查询、参数校验、统一错误响应和数据库迁移的后端模块',
     'openatom-system 或实验室系统后端中的真实 Issue',
     'https://spring.io/guides/'),
    ('python-ai', 'python-ai-growth', 'Python 与人工智能路线',
     '学习 Python 数据处理和机器学习基本流程，完成可复现的模型或智能应用。',
     'Python、NumPy/Pandas、数据清洗、训练验证划分和基础模型评估',
     '一个含数据说明、训练脚本、指标评估和推理入口的可复现实验',
     '社团 AI 项目、数据工具或开源模型应用中的真实 Issue',
     'https://scikit-learn.org/stable/user_guide'),
    ('ui-ux', 'ui-ux-growth', 'UI/UX 产品设计路线',
     '从需求理解、信息架构和界面规范出发，完成可交付、可验证的产品设计贡献。',
     '用户目标、任务流程、信息层级、组件状态、排版和无障碍基础',
     '一套包含关键流程、组件状态、响应式页面和交付标注的高保真原型',
     '社团现有产品中的真实体验问题、设计系统或改版需求',
     'https://help.figma.com/hc/en-us/categories/360002051613'),
    ('product-community', 'product-community-growth', '产品与社区运营路线',
     '学习问题调研、方案设计、活动运营和效果复盘，让真实需求形成可执行闭环。',
     '用户访谈、问题定义、优先级、PRD、活动方案和数据复盘',
     '一份基于真实访谈的产品或活动方案，并完成一次小范围执行验证',
     '社团招新、活动、内容运营或内部工具中的真实改进事项',
     'https://www.atlassian.com/agile/product-management'),
    ('technical-writing', 'technical-writing-growth', '技术文档工程路线',
     '掌握面向读者的技术表达、教程验证和文档维护流程，持续改善项目可用性。',
     '读者分析、信息结构、步骤写作、代码示例、链接检查和文档评审',
     '一篇从零可复现的安装或功能教程，包含前置条件、步骤、验证和排错',
     '社团项目 README、部署指南、API 文档或用户手册中的真实缺口',
     'https://developers.google.com/tech-writing'),
    ('testing-qa', 'testing-qa-growth', '测试与质量保障路线',
     '从测试分析和缺陷表达进入自动化、持续集成和真实项目质量改进。',
     '等价类、边界值、测试用例、缺陷报告、接口测试和基础自动化',
     '一套覆盖正常、异常和边界场景的测试方案，并实现关键流程自动化',
     '社团线上系统中的真实质量风险、回归缺口或自动化测试任务',
     'https://playwright.dev/docs/intro');

INSERT INTO quest_growth_route (
    route_key, name, description, direction_id, status, created_by, published_at
)
SELECT seed.route_key, seed.route_name, seed.route_description, direction.id,
       'PUBLISHED', @quest_seed_owner, NOW(3)
FROM quest_seed_route seed
JOIN quest_technical_direction direction ON direction.direction_key = seed.direction_key
WHERE NOT EXISTS (
    SELECT 1 FROM quest_growth_route route WHERE route.route_key = seed.route_key
);

INSERT INTO quest_route_stage (
    route_id, stage_id, sort_order, required_task_count, optional_task_count
)
SELECT route.id, stage.id,
       CASE stage.stage_key WHEN 'L0' THEN 1 WHEN 'L1' THEN 2 WHEN 'L2' THEN 3 WHEN 'L3' THEN 4 ELSE 5 END,
       1, 0
FROM quest_seed_route seed
JOIN quest_growth_route route ON route.route_key = seed.route_key
JOIN quest_growth_stage stage ON stage.stage_key IN ('L0', 'L1', 'L2', 'L3', 'L4')
WHERE NOT EXISTS (
    SELECT 1 FROM quest_route_stage route_stage
    WHERE route_stage.route_id = route.id AND route_stage.stage_id = stage.id
);

INSERT INTO quest_task (
    task_key, title, summary, direction_id, route_id, stage_id, task_type, difficulty,
    learning_objectives_json, estimated_minutes, deadline_type, duration_hours,
    instructions, resources_json, submission_requirements, acceptance_criteria, points,
    owner_member_id, faq_json, submission_limit, capacity, assignment_mode,
    required_in_stage, status, published_at, created_by
)
SELECT
    CONCAT(seed.route_key, '-', LOWER(stage.stage_key)),
    CONCAT(seed.route_name, ' · ', stage.stage_key, ' ',
        CASE stage.stage_key
            WHEN 'L0' THEN '完成路线报到与学习计划'
            WHEN 'L1' THEN '完成第一次规范化 Git 协作'
            WHEN 'L2' THEN '掌握方向核心基础'
            WHEN 'L3' THEN '交付一个完整实践作品'
            ELSE '完成一次真实项目贡献'
        END),
    CASE stage.stage_key
        WHEN 'L0' THEN CONCAT('了解', seed.route_name, '的成长目标、工具链和交付方式，建立个人学习计划。')
        WHEN 'L1' THEN CONCAT('使用 Git 和代码托管平台记录一次', seed.route_name, '学习实践，掌握可追溯协作流程。')
        WHEN 'L2' THEN CONCAT('围绕“', seed.foundation_topic, '”完成基础练习并解释关键概念。')
        WHEN 'L3' THEN CONCAT('独立完成', seed.practice_artifact, '，形成可以运行和复核的阶段作品。')
        ELSE CONCAT('在', seed.contribution_target, '中完成一次被维护者认可的真实贡献。')
    END,
    direction.id,
    route.id,
    stage.id,
    CASE stage.stage_key WHEN 'L0' THEN 'ONBOARDING' WHEN 'L1' THEN 'LEARNING'
        WHEN 'L2' THEN 'LEARNING' WHEN 'L3' THEN 'CHALLENGE' ELSE 'REAL_PROJECT' END,
    CASE stage.stage_key WHEN 'L0' THEN 'ENTRY' WHEN 'L1' THEN 'ENTRY'
        WHEN 'L2' THEN 'BEGINNER' WHEN 'L3' THEN 'INTERMEDIATE' ELSE 'ADVANCED' END,
    CASE stage.stage_key
        WHEN 'L0' THEN JSON_ARRAY('理解路线各阶段目标', '完成开发或创作环境检查', '制定两周可执行学习计划')
        WHEN 'L1' THEN JSON_ARRAY('掌握仓库克隆、分支、提交和推送', '编写清晰的提交信息', '通过 README 记录过程')
        WHEN 'L2' THEN JSON_ARRAY(CONCAT('掌握：', seed.foundation_topic), '能够解释关键实现选择', '形成可复现的基础练习')
        WHEN 'L3' THEN JSON_ARRAY(CONCAT('交付：', seed.practice_artifact), '处理加载、空数据、异常或边界场景', '完成自测与使用说明')
        ELSE JSON_ARRAY(CONCAT('参与：', seed.contribution_target), '按项目规范沟通和提交变更', '响应评审并完成必要修改')
    END,
    CASE stage.stage_key WHEN 'L0' THEN 45 WHEN 'L1' THEN 90 WHEN 'L2' THEN 240 WHEN 'L3' THEN 480 ELSE 600 END,
    'AFTER_CLAIM',
    CASE stage.stage_key WHEN 'L0' THEN 72 WHEN 'L1' THEN 120 WHEN 'L2' THEN 168 WHEN 'L3' THEN 240 ELSE 336 END,
    CASE stage.stage_key
        WHEN 'L0' THEN CONCAT(
            '1. 阅读路线说明，写下你希望解决的问题和最终目标。\n',
            '2. 根据方向准备必要的软件、账号或设计工具，并记录版本信息。\n',
            '3. 建立一个公开或社团可访问的学习仓库，创建 README。\n',
            '4. 在 README 中列出未来两周至少 4 个可执行学习动作。\n',
            '5. 完成一次环境验证，并记录结果、截图或输出。')
        WHEN 'L1' THEN CONCAT(
            '1. 克隆或创建学习仓库，配置 Git 用户信息。\n',
            '2. 创建独立分支，完成一项与', seed.route_name, '相关的小练习。\n',
            '3. 至少形成 2 次含义清晰的提交，不提交生成物或敏感信息。\n',
            '4. 推送分支并创建 Issue 或 Pull Request，说明目标、变更和验证。\n',
            '5. 根据一次自查或同伴意见补充修改。')
        WHEN 'L2' THEN CONCAT(
            '1. 阅读参考资料，整理“', seed.foundation_topic, '”的知识清单。\n',
            '2. 为每个核心知识点编写一个最小示例或设计说明。\n',
            '3. 将示例组合成一个可以独立运行或查看的基础作品。\n',
            '4. 补充 README：环境、启动方式、目录说明、关键实现和已知限制。\n',
            '5. 完成正常、异常和边界场景自测并记录结果。')
        WHEN 'L3' THEN CONCAT(
            '1. 明确作品范围和验收清单：', seed.practice_artifact, '。\n',
            '2. 先拆分数据、交互或模块边界，再按小步提交实现。\n',
            '3. 补齐输入校验、失败反馈、空状态和关键边界场景。\n',
            '4. 使用真实或可说明来源的示例数据完成端到端验证。\n',
            '5. 提供运行方式、演示材料、测试结果和一段复盘。')
        ELSE CONCAT(
            '1. 从', seed.contribution_target, '中选择范围明确且尚未被占用的问题。\n',
            '2. 先在 Issue 中确认现象、目标、边界和验收方式。\n',
            '3. 按项目贡献规范实现，并保持提交聚焦、可审查。\n',
            '4. 提交 Pull Request 或等价交付，附验证证据并响应评审。\n',
            '5. 贡献合并或被维护者确认后，总结影响、反馈和后续工作。')
    END,
    JSON_ARRAY(seed.reference_url, 'https://docs.github.com/zh/get-started'),
    CASE stage.stage_key
        WHEN 'L0' THEN '提交学习仓库地址；说明环境验证结果；粘贴两周学习计划和至少一张有效截图。'
        WHEN 'L1' THEN '提交仓库、Issue 或 Pull Request 地址；说明分支名、提交记录、自查内容和修改结果。'
        WHEN 'L2' THEN '提交仓库或设计文件地址；说明完成的知识点、运行或查看方式、测试结果和遇到的问题。'
        WHEN 'L3' THEN '提交仓库地址，并至少提供在线演示、截图或视频之一；说明功能范围、测试结果和复盘。'
        ELSE '提交真实 Issue 与 Pull Request 或等价贡献地址；说明本人贡献、评审过程、验证证据和最终结果。'
    END,
    CASE stage.stage_key
        WHEN 'L0' THEN '仓库可访问；工具环境验证有效；计划至少包含 4 个有时间边界的动作；内容与所选方向一致。'
        WHEN 'L1' THEN '存在独立分支和至少 2 次有效提交；提交信息清晰；Issue 或 PR 说明完整；变更可复核。'
        WHEN 'L2' THEN CONCAT('覆盖方向基础主题：', seed.foundation_topic, '；示例可运行或设计可查看；README 和测试记录完整。')
        WHEN 'L3' THEN CONCAT('作品达到约定范围：', seed.practice_artifact, '；核心流程可用；异常与边界处理明确；他人可按说明复现。')
        ELSE '贡献对象真实存在；沟通和提交符合项目规范；有维护者反馈或合并记录；能够清楚说明个人贡献和验证方法。'
    END,
    CASE stage.stage_key WHEN 'L0' THEN 50 WHEN 'L1' THEN 100 WHEN 'L2' THEN 200 WHEN 'L3' THEN 450 ELSE 800 END,
    @quest_seed_owner,
    CASE stage.stage_key
        WHEN 'L0' THEN JSON_ARRAY('暂时没有完整环境也可以先提交安装记录和阻塞信息。', '计划应具体到可执行动作，不要只写“学习基础”。')
        WHEN 'L1' THEN JSON_ARRAY('可以使用 GitHub 或 Gitee，需保证导师能够访问。', '误提交生成物时请新增修复提交，不要隐藏过程。')
        WHEN 'L2' THEN JSON_ARRAY('允许查阅资料和使用 AI，但必须理解并验证最终内容。', '无法运行的设计方向应提供可检查的原型和说明。')
        WHEN 'L3' THEN JSON_ARRAY('作品范围宁可小而完整，不要求堆叠大量功能。', '第三方素材和数据需要注明来源。')
        ELSE JSON_ARRAY('贡献未合并时，可提交维护者确认、评审记录或可验证的阶段结果。', '不要未经沟通直接提交大范围重构。')
    END,
    3,
    NULL,
    'INDIVIDUAL',
    TRUE,
    'PUBLISHED',
    NOW(3),
    @quest_seed_owner
FROM quest_seed_route seed
JOIN quest_technical_direction direction ON direction.direction_key = seed.direction_key
JOIN quest_growth_route route ON route.route_key = seed.route_key
JOIN quest_growth_stage stage ON stage.stage_key IN ('L0', 'L1', 'L2', 'L3', 'L4')
WHERE NOT EXISTS (
    SELECT 1 FROM quest_task task
    WHERE task.task_key = CONCAT(seed.route_key, '-', LOWER(stage.stage_key))
);

INSERT INTO quest_task_prerequisite (task_id, prerequisite_task_id)
SELECT current_task.id, previous_task.id
FROM quest_seed_route seed
JOIN quest_growth_route route ON route.route_key = seed.route_key
JOIN quest_route_stage current_route_stage ON current_route_stage.route_id = route.id
JOIN quest_route_stage previous_route_stage
    ON previous_route_stage.route_id = route.id
   AND previous_route_stage.sort_order = current_route_stage.sort_order - 1
JOIN quest_growth_stage current_stage ON current_stage.id = current_route_stage.stage_id
JOIN quest_growth_stage previous_stage ON previous_stage.id = previous_route_stage.stage_id
JOIN quest_task current_task
    ON current_task.task_key = CONCAT(seed.route_key, '-', LOWER(current_stage.stage_key))
JOIN quest_task previous_task
    ON previous_task.task_key = CONCAT(seed.route_key, '-', LOWER(previous_stage.stage_key))
WHERE NOT EXISTS (
    SELECT 1 FROM quest_task_prerequisite prerequisite
    WHERE prerequisite.task_id = current_task.id
      AND prerequisite.prerequisite_task_id = previous_task.id
);

DROP TEMPORARY TABLE quest_seed_route;
