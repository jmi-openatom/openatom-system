-- 每位成员在三个页面各有一枚随机标记，提交后由服务端自动核验。
CREATE TABLE quest_site_exploration_flag (
    member_id BIGINT NOT NULL,
    page_key VARCHAR(24) NOT NULL,
    flag_value VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (member_id, page_key),
    UNIQUE KEY uk_quest_site_exploration_flag_value (flag_value),
    CONSTRAINT fk_quest_site_exploration_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_site_exploration_page CHECK (page_key IN ('about', 'regulations', 'activities'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 所有方向共用的第一项 L0 任务。
SET @quest_site_exploration_owner = COALESCE(
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

INSERT INTO quest_task (
    task_key, title, summary, stage_id, task_type, difficulty,
    learning_objectives_json, estimated_minutes, deadline_type, instructions,
    resources_json, submission_requirements, acceptance_criteria, points,
    owner_member_id, faq_json, assignment_mode, required_in_stage, status,
    published_at, created_by
)
SELECT
    'site-exploration-l0',
    '主站探索：认识开放原子开源社团',
    '带着三条线索浏览社团主站，找到属于你的探索标记，认识社团、协作规则和参与方式。',
    stage.id,
    'ONBOARDING',
    'ENTRY',
    JSON_ARRAY('了解社团的目标与成长方式', '找到公开的协作规则', '知道从哪里发现和参与活动'),
    15,
    'NONE',
    CONCAT(
        '1. 打开主站「关于我们」，了解社团与新人路线，寻找第 1 枚探索标记。\n',
        '2. 打开「规章制度」，浏览已发布制度与协作规范，寻找第 2 枚探索标记。\n',
        '3. 打开「社团活动」，了解活动信息与报名方式，寻找第 3 枚探索标记。\n',
        '4. 回到 Quest，在三个输入框中分别填写标记，并写下你最想参与的事和准备怎样开始。三个标记会立即自动核验。'
    ),
    JSON_ARRAY('https://www.jmi-openatom.cn/about', 'https://www.jmi-openatom.cn/regulations', 'https://www.jmi-openatom.cn/activities'),
    '分别提交你在 3 个页面看到的个人探索标记；用自己的话写 1—2 句话，说明你最想参与什么、准备怎样开始。不需要仓库或截图。',
    '服务端自动核验 3 枚个人标记；填写至少 8 个字的参与计划后立即通过并发放积分。',
    20,
    @quest_site_exploration_owner,
    JSON_ARRAY('先登录 Quest，再打开主站页面，标记会按你的 Quest 账号显示。', '标记在页面内容中，手机端也能看到。', '活动或制度列表暂时为空时，仍可在对应页面找到探索标记。'),
    'INDIVIDUAL',
    TRUE,
    'PUBLISHED',
    NOW(3),
    @quest_site_exploration_owner
FROM quest_growth_stage stage
WHERE stage.stage_key = 'L0'
  AND NOT EXISTS (SELECT 1 FROM quest_task WHERE task_key = 'site-exploration-l0');

-- 已发布的九条方向路线先完成主站探索，再领取各自的 L0 任务。
INSERT INTO quest_task_prerequisite (task_id, prerequisite_task_id)
SELECT route_task.id, exploration_task.id
FROM quest_task exploration_task
JOIN quest_growth_route route ON route.status = 'PUBLISHED'
JOIN quest_task route_task
    ON route_task.route_id = route.id
   AND route_task.task_key = CONCAT(route.route_key, '-l0')
WHERE exploration_task.task_key = 'site-exploration-l0'
  AND NOT EXISTS (
      SELECT 1 FROM quest_task_prerequisite prerequisite
      WHERE prerequisite.task_id = route_task.id
        AND prerequisite.prerequisite_task_id = exploration_task.id
  );
