SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- 用户表
CREATE TABLE IF NOT EXISTS `tb_user`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `user_name`
    VARCHAR
(
    50
) NOT NULL COMMENT '用户名',
    `password` VARCHAR
(
    255
) NOT NULL COMMENT '密码',
    `real_name` VARCHAR
(
    50
) DEFAULT NULL COMMENT '真实姓名',
    `gender` VARCHAR
(
    10
) DEFAULT NULL COMMENT '性别',
    `phone` VARCHAR
(
    20
) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR
(
    100
) DEFAULT NULL COMMENT '邮箱',
    `student_id` VARCHAR
(
    30
) DEFAULT NULL COMMENT '学号/工号',
    `college` VARCHAR
(
    100
) DEFAULT NULL COMMENT '学院',
    `major` VARCHAR
(
    100
) DEFAULT NULL COMMENT '专业',
    `grade` VARCHAR
(
    20
) DEFAULT NULL COMMENT '年级',
    `class_name` VARCHAR
(
    100
) DEFAULT NULL COMMENT '班级',
    `avatar` VARCHAR
(
    255
) DEFAULT NULL COMMENT '头像URL',
    `miniapp_openid` VARCHAR
(
    80
) DEFAULT NULL COMMENT '微信小程序openid',
    `wechat_unionid` VARCHAR
(
    80
) DEFAULT NULL COMMENT '微信unionid',
    `user_status` TINYINT DEFAULT 1 COMMENT '用户状态: 1-active, 0-disabled, 2-locked',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_login_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_user_name`
(
    `user_name`
),
    UNIQUE KEY `uk_student_id`
(
    `student_id`
),
    UNIQUE KEY `uk_miniapp_openid`
(
    `miniapp_openid`
),
    KEY `idx_user_status`
(
    `user_status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `name`
    VARCHAR
(
    50
) NOT NULL COMMENT '角色名称',
    `code` VARCHAR
(
    50
) NOT NULL COMMENT '角色编码',
    `data_scope` VARCHAR
(
    255
) DEFAULT NULL COMMENT '数据权限范围',
    `description` VARCHAR
(
    255
) DEFAULT NULL COMMENT '描述',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_role_code`
(
    `code`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `name`
    VARCHAR
(
    50
) NOT NULL COMMENT '权限名称',
    `code` VARCHAR
(
    80
) NOT NULL COMMENT '权限编码',
    `type` VARCHAR
(
    20
) DEFAULT NULL COMMENT '类型: menu/page/button/api/data',
    `path` VARCHAR
(
    255
) DEFAULT NULL COMMENT '路由地址/接口路径',
    `method` VARCHAR
(
    10
) DEFAULT NULL COMMENT '请求方式',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_perm_code`
(
    `code`
),
    KEY `idx_perm_path_method`
(
    `path`,
    `method`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `role_id`
    INT
    NOT
    NULL
    COMMENT
    '角色ID',
    `permission_id`
    INT
    NOT
    NULL
    COMMENT
    '权限ID',
    PRIMARY
    KEY
(
    `role_id`,
    `permission_id`
),
    KEY `idx_rp_permission_id`
(
    `permission_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `user_id`
    INT
    NOT
    NULL
    COMMENT
    '用户ID',
    `role_id`
    INT
    NOT
    NULL
    COMMENT
    '角色ID',
    PRIMARY
    KEY
(
    `user_id`,
    `role_id`
),
    KEY `idx_ur_role_id`
(
    `role_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- 社团表
CREATE TABLE IF NOT EXISTS `club`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `name`
    VARCHAR
(
    100
) NOT NULL COMMENT '社团名称',
    `code` VARCHAR
(
    50
) NOT NULL COMMENT '社团编码',
    `category` VARCHAR
(
    50
) DEFAULT NULL COMMENT '分类',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `logo_url` VARCHAR
(
    255
) DEFAULT NULL COMMENT 'Logo URL',
    `president_user_id` INT DEFAULT NULL COMMENT '负责人用户ID',
    `status` VARCHAR
(
    20
) DEFAULT 'active' COMMENT '社团状态: active/inactive',
    `recruitment_status` VARCHAR
(
    20
) DEFAULT 'closed' COMMENT '招新状态: open/closed',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_club_code`
(
    `code`
),
    KEY `idx_club_status`
(
    `status`
),
    KEY `idx_club_recruitment_status`
(
    `recruitment_status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团表';

-- 部门表
CREATE TABLE IF NOT EXISTS `club_department`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `name`
    VARCHAR
(
    100
) NOT NULL COMMENT '部门名称',
    `description` VARCHAR
(
    255
) DEFAULT NULL COMMENT '描述',
    `manager_user_id` INT DEFAULT NULL COMMENT '负责人用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_department_club_id`
(
    `club_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团部门表';

-- 岗位表
CREATE TABLE IF NOT EXISTS `club_position`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `department_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '部门ID',
    `name`
    VARCHAR
(
    100
) NOT NULL COMMENT '岗位名称',
    `max_count` INT DEFAULT NULL COMMENT '人数上限',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_position_club_id`
(
    `club_id`
),
    KEY `idx_position_department_id`
(
    `department_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团岗位表';

-- 岗位角色关联表
CREATE TABLE IF NOT EXISTS `club_position_role`
(
    `position_id`
    INT
    NOT
    NULL
    COMMENT
    '岗位ID',
    `role_id`
    INT
    NOT
    NULL
    COMMENT
    '角色ID',
    PRIMARY
    KEY
(
    `position_id`,
    `role_id`
),
    KEY `idx_position_role_role_id`
(
    `role_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='岗位角色关联表';

-- 招新计划表
CREATE TABLE IF NOT EXISTS `recruitment_campaign`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `name`
    VARCHAR
(
    100
) NOT NULL COMMENT '招新计划名称',
    `apply_start_at` TIMESTAMP NOT NULL COMMENT '申请开始时间',
    `apply_end_at` TIMESTAMP NOT NULL COMMENT '申请结束时间',
    `interview_start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '面试开始时间',
    `interview_end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '面试结束时间',
    `result_publish_at` TIMESTAMP NULL DEFAULT NULL COMMENT '结果发布时间',
    `target_grades` JSON DEFAULT NULL COMMENT '面向年级',
    `max_applicants` INT DEFAULT NULL COMMENT '申请人数上限',
    `login_required` TINYINT
(
    1
) DEFAULT 1 COMMENT '是否要求登录后提交',
    `form_schema` JSON DEFAULT NULL COMMENT '自定义报名表单结构',
    `status` VARCHAR
(
    30
) DEFAULT 'draft' COMMENT '状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_campaign_club_id`
(
    `club_id`
),
    KEY `idx_campaign_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='招新计划表';

-- 信息收集表单表
CREATE TABLE IF NOT EXISTS `site_form`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `name`
    VARCHAR
(
    100
) NOT NULL COMMENT '表单名称',
    `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '收集开始时间',
    `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '收集结束时间',
    `login_required` TINYINT
(
    1
) DEFAULT 1 COMMENT '是否要求登录后提交',
    `form_schema` JSON DEFAULT NULL COMMENT '自定义表单结构',
    `status` VARCHAR
(
    30
) DEFAULT 'draft' COMMENT '状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_site_form_club_id`
(
    `club_id`
),
    KEY `idx_site_form_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='信息收集表单表';

-- 入会申请表
CREATE TABLE IF NOT EXISTS `membership_application`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `campaign_id`
    INT
    NOT
    NULL
    COMMENT
    '招新计划ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `user_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '申请用户ID，匿名提交时可为空',
    `first_choice_department_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '第一志愿部门ID',
    `second_choice_department_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '第二志愿部门ID',
    `status`
    VARCHAR
(
    40
) DEFAULT 'draft' COMMENT '申请状态',
    `profile` JSON DEFAULT NULL COMMENT '申请表单扩展信息',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_application_campaign_id`
(
    `campaign_id`
),
    KEY `idx_application_club_id`
(
    `club_id`
),
    KEY `idx_application_user_id`
(
    `user_id`
),
    KEY `idx_application_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='入会申请表';

-- 自定义表单提交记录
CREATE TABLE IF NOT EXISTS `form_submission`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `form_id`
    INT
    NOT
    NULL
    COMMENT
    '表单ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `user_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '用户ID',
    `anonymous_name`
    VARCHAR
(
    50
) DEFAULT NULL COMMENT '匿名姓名',
    `anonymous_contact` VARCHAR
(
    100
) DEFAULT NULL COMMENT '匿名联系方式',
    `form_data` JSON DEFAULT NULL COMMENT '提交的表单数据',
    `status` VARCHAR
(
    30
) DEFAULT 'submitted' COMMENT '状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_form_form_id`
(
    `form_id`
),
    KEY `idx_form_club_id`
(
    `club_id`
),
    KEY `idx_form_user_id`
(
    `user_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='自定义表单提交记录';

-- 审批记录表
CREATE TABLE IF NOT EXISTS `approval_record`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `application_id`
    INT
    NOT
    NULL
    COMMENT
    '申请ID',
    `node`
    VARCHAR
(
    50
) NOT NULL COMMENT '审批节点',
    `action` VARCHAR
(
    40
) NOT NULL COMMENT '审批动作',
    `operator_id` INT DEFAULT NULL COMMENT '操作人ID',
    `comment` VARCHAR
(
    500
) DEFAULT NULL COMMENT '审批意见',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_approval_application_id`
(
    `application_id`
),
    KEY `idx_approval_operator_id`
(
    `operator_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='审批记录表';

-- 面试安排表
CREATE TABLE IF NOT EXISTS `interview`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `application_id`
    INT
    NOT
    NULL
    COMMENT
    '申请ID',
    `round`
    INT
    NOT
    NULL
    COMMENT
    '轮次',
    `scheduled_start_at`
    TIMESTAMP
    NOT
    NULL
    COMMENT
    '计划开始时间',
    `scheduled_end_at`
    TIMESTAMP
    NOT
    NULL
    COMMENT
    '计划结束时间',
    `location`
    VARCHAR
(
    255
) DEFAULT NULL COMMENT '地点',
    `mode` VARCHAR
(
    20
) DEFAULT NULL COMMENT '方式: online/offline',
    `status` VARCHAR
(
    30
) DEFAULT 'pending' COMMENT '状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_interview_application_id`
(
    `application_id`
),
    KEY `idx_interview_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='面试安排表';

-- 面试官关联表
CREATE TABLE IF NOT EXISTS `interview_interviewer`
(
    `interview_id`
    INT
    NOT
    NULL
    COMMENT
    '面试ID',
    `interviewer_id`
    INT
    NOT
    NULL
    COMMENT
    '面试官用户ID',
    PRIMARY
    KEY
(
    `interview_id`,
    `interviewer_id`
),
    KEY `idx_interviewer_user_id`
(
    `interviewer_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='面试官关联表';

-- 面试反馈表
CREATE TABLE IF NOT EXISTS `interview_feedback`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `interview_id`
    INT
    NOT
    NULL
    COMMENT
    '面试ID',
    `interviewer_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '面试官用户ID',
    `scores`
    JSON
    DEFAULT
    NULL
    COMMENT
    '评分JSON',
    `suggestion`
    VARCHAR
(
    30
) DEFAULT NULL COMMENT '建议: pass/reject/waitlist',
    `comment` VARCHAR
(
    500
) DEFAULT NULL COMMENT '评语',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_feedback_interview_id`
(
    `interview_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='面试反馈表';

-- 社团成员表
CREATE TABLE IF NOT EXISTS `club_membership`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `user_id`
    INT
    NOT
    NULL
    COMMENT
    '用户ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `department_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '部门ID',
    `position_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '岗位ID',
    `status`
    VARCHAR
(
    30
) DEFAULT 'probation' COMMENT '成员状态',
    `featured` TINYINT
(
    1
) DEFAULT 0 COMMENT '是否在官网主要人员展示',
    `sort_order` INT DEFAULT 0 COMMENT '官网展示排序',
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `left_at` TIMESTAMP NULL DEFAULT NULL COMMENT '离开时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_membership_user_id`
(
    `user_id`
),
    KEY `idx_membership_club_id`
(
    `club_id`
),
    KEY `idx_membership_status`
(
    `status`
),
    KEY `idx_membership_featured`
(
    `featured`,
    `sort_order`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团成员表';

-- 社团活动表
CREATE TABLE IF NOT EXISTS `club_activity`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `title`
    VARCHAR
(
    120
) NOT NULL COMMENT '活动标题',
    `summary` VARCHAR
(
    500
) DEFAULT NULL COMMENT '活动摘要',
    `description_markdown` MEDIUMTEXT DEFAULT NULL COMMENT 'Markdown 活动介绍',
    `activity_at` TIMESTAMP NULL DEFAULT NULL COMMENT '活动开始时间',
    `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '活动结束时间',
    `location` VARCHAR
(
    120
) DEFAULT NULL COMMENT '活动地点',
    `status` VARCHAR
(
    30
) DEFAULT 'draft' COMMENT '状态: draft/published/closed/cancelled',
    `cover_url` VARCHAR
(
    255
) DEFAULT NULL COMMENT '封面URL',
    `registration_required` TINYINT
(
    1
) DEFAULT 0 COMMENT '是否需要官网报名',
    `registration_start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '报名开始时间',
    `registration_end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '报名结束时间',
    `registration_fields` JSON DEFAULT NULL COMMENT '自定义报名字段',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_activity_club_time`
(
    `club_id`,
    `activity_at`
),
    KEY `idx_activity_status`
(
    `status`
),
    KEY `idx_activity_registration_required`
(
    `registration_required`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团活动表';

CREATE TABLE IF NOT EXISTS `activity_registration`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `activity_id`
    INT
    NOT
    NULL
    COMMENT
    '活动ID',
    `user_id`
    INT
    NOT
    NULL
    COMMENT
    '报名用户ID',
    `form_data`
    JSON
    DEFAULT
    NULL
    COMMENT
    '报名表单内容',
    `status`
    VARCHAR
(
    30
) DEFAULT 'submitted' COMMENT '状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_activity_registration_user`
(
    `activity_id`,
    `user_id`
),
    KEY `idx_activity_registration_activity`
(
    `activity_id`
),
    KEY `idx_activity_registration_user`
(
    `user_id`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='活动报名表';

CREATE TABLE IF NOT EXISTS `checkin_session`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `activity_id` INT DEFAULT NULL COMMENT '关联活动ID',
    `group_id` INT DEFAULT NULL COMMENT '签到分组ID',
    `title` VARCHAR(120) NOT NULL COMMENT '签到标题',
    `location` VARCHAR(120) DEFAULT NULL COMMENT '签到地点',
    `start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '签到开始时间',
    `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '签到结束时间',
    `status` VARCHAR(30) DEFAULT 'open' COMMENT '状态: draft/open/closed',
    `token` VARCHAR(64) NOT NULL COMMENT '扫码签到令牌',
    `created_by` INT DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkin_session_token` (`token`),
    KEY `idx_checkin_session_club` (`club_id`),
    KEY `idx_checkin_session_activity` (`activity_id`),
    KEY `idx_checkin_session_group` (`group_id`),
    KEY `idx_checkin_session_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到场次表';

CREATE TABLE IF NOT EXISTS `checkin_group`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `club_id` INT NOT NULL COMMENT '社团ID',
    `name` VARCHAR(120) NOT NULL COMMENT '分组名称',
    `created_by` INT DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_checkin_group_club` (`club_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组表';

CREATE TABLE IF NOT EXISTS `checkin_group_member`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id` INT NOT NULL COMMENT '签到分组ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkin_group_member_user` (`group_id`, `user_id`),
    KEY `idx_checkin_group_member_group` (`group_id`),
    KEY `idx_checkin_group_member_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='签到分组成员表';

CREATE TABLE IF NOT EXISTS `checkin_target`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` INT NOT NULL COMMENT '签到场次ID',
    `user_id` INT NOT NULL COMMENT '发放用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkin_target_user` (`session_id`, `user_id`),
    KEY `idx_checkin_target_session` (`session_id`),
    KEY `idx_checkin_target_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到发放人员表';

CREATE TABLE IF NOT EXISTS `checkin_record`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `session_id` INT NOT NULL COMMENT '签到场次ID',
    `user_id` INT NOT NULL COMMENT '签到用户ID',
    `checkin_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
    `source` VARCHAR(40) DEFAULT 'miniapp_scan' COMMENT '签到来源',
    `status` VARCHAR(30) DEFAULT 'checked' COMMENT '签到状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkin_record_user` (`session_id`, `user_id`),
    KEY `idx_checkin_record_session` (`session_id`),
    KEY `idx_checkin_record_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内部签到记录表';

-- 社团比赛获奖表
CREATE TABLE IF NOT EXISTS `club_award`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `title`
    VARCHAR
(
    120
) NOT NULL COMMENT '获奖标题',
    `competition_name` VARCHAR
(
    160
) NOT NULL COMMENT '比赛名称',
    `award_level` VARCHAR
(
    80
) DEFAULT NULL COMMENT '奖项等级',
    `award_year` INT DEFAULT NULL COMMENT '获奖年份',
    `team_name` VARCHAR
(
    120
) DEFAULT NULL COMMENT '队伍/成员',
    `description` VARCHAR
(
    500
) DEFAULT NULL COMMENT '获奖说明',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_award_club_year`
(
    `club_id`,
    `award_year`
),
    KEY `idx_award_sort_order`
(
    `sort_order`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='社团比赛获奖表';

-- 退社申请表
CREATE TABLE IF NOT EXISTS `exit_application`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `membership_id`
    INT
    NOT
    NULL
    COMMENT
    '成员关系ID',
    `reason`
    VARCHAR
(
    255
) NOT NULL COMMENT '退社原因',
    `description` TEXT DEFAULT NULL COMMENT '说明',
    `status` VARCHAR
(
    30
) DEFAULT 'submitted' COMMENT '状态: submitted/approved/rejected',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_exit_membership_id`
(
    `membership_id`
),
    KEY `idx_exit_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='退社申请表';

-- 通知表
CREATE TABLE IF NOT EXISTS `notification`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `title`
    VARCHAR
(
    100
) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `type` VARCHAR
(
    30
) DEFAULT NULL COMMENT '通知类型',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_notification_type`
(
    `type`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='通知表';

-- 用户通知关联表
CREATE TABLE IF NOT EXISTS `notification_receiver`
(
    `notification_id`
    INT
    NOT
    NULL
    COMMENT
    '通知ID',
    `receiver_user_id`
    INT
    NOT
    NULL
    COMMENT
    '接收人用户ID',
    `read_flag`
    TINYINT
    DEFAULT
    0
    COMMENT
    '是否已读',
    `read_at`
    TIMESTAMP
    NULL
    DEFAULT
    NULL
    COMMENT
    '阅读时间',
    PRIMARY
    KEY
(
    `notification_id`,
    `receiver_user_id`
),
    KEY `idx_notification_receiver_user_id`
(
    `receiver_user_id`
),
    KEY `idx_notification_read_flag`
(
    `read_flag`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='通知接收表';

-- 办公文书表
CREATE TABLE IF NOT EXISTS `office_document`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `club_id`
    INT
    NOT
    NULL
    COMMENT
    '社团ID',
    `doc_type`
    VARCHAR
(
    50
) NOT NULL COMMENT '文书类型',
    `title` VARCHAR
(
    255
) NOT NULL COMMENT '文书标题',
    `status` VARCHAR
(
    30
) DEFAULT 'draft' COMMENT '状态',
    `payload` MEDIUMTEXT DEFAULT NULL COMMENT '文书内容/配置',
    `created_by` INT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` INT DEFAULT NULL COMMENT '更新人ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_doc_club_id`
(
    `club_id`
),
    KEY `idx_doc_type`
(
    `doc_type`
),
    KEY `idx_doc_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='办公文书表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `operator_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '操作人ID',
    `module`
    VARCHAR
(
    50
) DEFAULT NULL COMMENT '模块',
    `action` VARCHAR
(
    120
) DEFAULT NULL COMMENT '动作',
    `target_id` VARCHAR
(
    80
) DEFAULT NULL COMMENT '目标ID',
    `content` TEXT DEFAULT NULL COMMENT '操作内容',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_operation_operator_id`
(
    `operator_id`
),
    KEY `idx_operation_module_action`
(
    `module`,
    `action`
),
    KEY `idx_operation_created_at`
(
    `created_at`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS `login_log`
(
    `id`
    INT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键ID',
    `user_id`
    INT
    DEFAULT
    NULL
    COMMENT
    '用户ID',
    `login_at`
    TIMESTAMP
    DEFAULT
    CURRENT_TIMESTAMP
    COMMENT
    '登录时间',
    `ip`
    VARCHAR
(
    64
) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR
(
    500
) DEFAULT NULL COMMENT 'User-Agent',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_login_user_id`
(
    `user_id`
),
    KEY `idx_login_at`
(
    `login_at`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_setting`
(
    `setting_key`
    VARCHAR
(
    100
) NOT NULL COMMENT '配置键',
    `setting_value` VARCHAR
(
    255
) DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR
(
    255
) DEFAULT NULL COMMENT '配置说明',
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `setting_key`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

-- 初始化权限点
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '退出登录', 'auth:logout', 'api', '/auth/logout', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'auth:logout');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查看当前用户信息', 'auth:me', 'api', '/auth/me', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'auth:me');

-- 角色与权限管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询角色列表', 'role:list', 'api', '/roles', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建角色', 'role:create', 'api', '/roles', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取角色详情', 'role:detail', 'api', '/roles/{roleId}', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新角色', 'role:update', 'api', '/roles/{roleId}', 'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除角色', 'role:delete', 'api', '/roles/{roleId}', 'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:delete');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '分配角色权限',
       'role:permission:assign',
       'api',
       '/roles/{roleId}/permissions',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'role:permission:assign');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询权限点列表', 'permission:list', 'api', '/permissions', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'permission:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建权限点', 'permission:create', 'api', '/permissions', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'permission:create');

-- 用户管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询用户列表', 'user:list', 'api', '/users', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '后台创建用户', 'user:create', 'api', '/users', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取用户详情', 'user:info', 'api', '/users/{userId}', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:info');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新用户信息', 'user:update', 'api', '/users/{userId}', 'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新用户状态',
       'user:status:update',
       'api',
       '/users/{userId}/status',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:status:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '重置用户密码',
       'user:password:reset',
       'api',
       '/users/{userId}/reset-password',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:password:reset');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '分配用户角色',
       'user:role:assign',
       'api',
       '/users/{userId}/roles',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:role:assign');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '导入用户与下载模板', 'user:import', 'api', '/users/import/**', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:import');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取用户社团关系',
       'user:membership:list',
       'api',
       '/users/{userId}/memberships',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'user:membership:list');

-- 社团管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询社团列表', 'club:list', 'api', '/clubs', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建社团', 'club:create', 'api', '/clubs', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取社团详情', 'club:detail', 'api', '/clubs/{clubId}', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新社团信息', 'club:update', 'api', '/clubs/{clubId}', 'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新社团状态',
       'club:status:update',
       'api',
       '/clubs/{clubId}/status',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:status:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新社团招新状态',
       'club:recruitment-status:update',
       'api',
       '/clubs/{clubId}/recruitment-status',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'club:recruitment-status:update');

-- 部门与岗位
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询部门列表',
       'department:list',
       'api',
       '/clubs/{clubId}/departments',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'department:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建部门',
       'department:create',
       'api',
       '/clubs/{clubId}/departments',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'department:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取部门详情',
       'department:detail',
       'api',
       '/departments/{departmentId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'department:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新部门',
       'department:update',
       'api',
       '/departments/{departmentId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'department:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除部门',
       'department:delete',
       'api',
       '/departments/{departmentId}',
       'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'department:delete');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询岗位列表',
       'position:list',
       'api',
       '/clubs/{clubId}/positions',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'position:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建岗位',
       'position:create',
       'api',
       '/clubs/{clubId}/positions',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'position:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取岗位详情',
       'position:detail',
       'api',
       '/positions/{positionId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'position:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新岗位',
       'position:update',
       'api',
       '/positions/{positionId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'position:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除岗位',
       'position:delete',
       'api',
       '/positions/{positionId}',
       'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'position:delete');

-- 招新管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询招新计划',
       'recruitment-campaign:list',
       'api',
       '/clubs/{clubId}/recruitment-campaigns',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建招新计划',
       'recruitment-campaign:create',
       'api',
       '/clubs/{clubId}/recruitment-campaigns',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取招新计划详情',
       'recruitment-campaign:detail',
       'api',
       '/recruitment-campaigns/{campaignId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询信息收集表单', 'site-form:list', 'api', '/site-forms', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'site-form:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '新增信息收集表单', 'site-form:create', 'api', '/site-forms', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'site-form:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新信息收集表单',
       'site-form:update',
       'api',
       '/site-forms/{formId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'site-form:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取表单详情',
       'site-form:detail',
       'api',
       '/site-forms/{formId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'site-form:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新招新计划',
       'recruitment-campaign:update',
       'api',
       '/recruitment-campaigns/{campaignId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '发布招新计划',
       'recruitment-campaign:publish',
       'api',
       '/recruitment-campaigns/{campaignId}/publish',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:publish');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '关闭招新计划',
       'recruitment-campaign:close',
       'api',
       '/recruitment-campaigns/{campaignId}/close',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'recruitment-campaign:close');

-- 申请与审批
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询入会申请', 'application:list', 'api', '/applications', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '提交入会申请', 'application:create', 'api', '/applications', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取入会申请详情',
       'application:detail',
       'api',
       '/applications/{applicationId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新入会申请',
       'application:update',
       'api',
       '/applications/{applicationId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '提交草稿申请',
       'application:submit',
       'api',
       '/applications/{applicationId}/submit',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:submit');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '撤回入会申请',
       'application:withdraw',
       'api',
       '/applications/{applicationId}/withdraw',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:withdraw');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询审批记录',
       'approval-record:list',
       'api',
       '/applications/{applicationId}/approval-records',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'approval-record:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '审批入会申请',
       'application:approve',
       'api',
       '/applications/{applicationId}/approvals',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:approve');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '批量审批入会申请',
       'application:batch-approve',
       'api',
       '/applications/batch-approvals',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:batch-approve');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '终审决策',
       'application:final-decision',
       'api',
       '/applications/{applicationId}/final-decisions',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'application:final-decision');

-- 面试管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询面试列表', 'interview:list', 'api', '/interviews', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建面试安排', 'interview:create', 'api', '/interviews', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取面试详情',
       'interview:detail',
       'api',
       '/interviews/{interviewId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新面试安排',
       'interview:update',
       'api',
       '/interviews/{interviewId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '确认面试',
       'interview:confirm',
       'api',
       '/interviews/{interviewId}/confirm',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:confirm');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '提交面试反馈',
       'interview:feedback',
       'api',
       '/interviews/{interviewId}/feedback',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:feedback');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '完成面试',
       'interview:complete',
       'api',
       '/interviews/{interviewId}/complete',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'interview:complete');

-- 活动与获奖
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询活动列表', 'activity:list', 'api', '/activities', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建活动', 'activity:create', 'api', '/activities', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取活动详情',
       'activity:detail',
       'api',
       '/activities/{activityId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新活动',
       'activity:update',
       'api',
       '/activities/{activityId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除活动',
       'activity:delete',
       'api',
       '/activities/{activityId}',
       'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity:delete');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询活动报名',
       'activity-registration:list',
       'api',
       '/activities/{activityId}/registrations',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'activity-registration:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询获奖经历', 'award:list', 'api', '/awards', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'award:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建获奖经历', 'award:create', 'api', '/awards', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'award:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新获奖经历', 'award:update', 'api', '/awards/{awardId}', 'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'award:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除获奖经历', 'award:delete', 'api', '/awards/{awardId}', 'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'award:delete');

-- 成员与退出
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询成员列表', 'membership:list', 'api', '/memberships', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '新增成员', 'membership:create', 'api', '/memberships', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取成员详情',
       'membership:detail',
       'api',
       '/memberships/{membershipId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新成员',
       'membership:update',
       'api',
       '/memberships/{membershipId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '分配成员岗位',
       'membership:position:assign',
       'api',
       '/memberships/{membershipId}/assign-position',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:position:assign');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '修改成员状态',
       'membership:status:change',
       'api',
       '/memberships/{membershipId}/change-status',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:status:change');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询退社申请',
       'exit-application:list',
       'api',
       '/exit-applications',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'exit-application:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '提交退社申请',
       'exit-application:create',
       'api',
       '/exit-applications',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'exit-application:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '获取退社申请详情',
       'exit-application:detail',
       'api',
       '/exit-applications/{exitApplicationId}',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'exit-application:detail');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '通过退社申请',
       'exit-application:approve',
       'api',
       '/exit-applications/{exitApplicationId}/approve',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'exit-application:approve');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '驳回退社申请',
       'exit-application:reject',
       'api',
       '/exit-applications/{exitApplicationId}/reject',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'exit-application:reject');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '强制退社',
       'membership:force-exit',
       'api',
       '/memberships/{membershipId}/force-exit',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'membership:force-exit');

-- 通知管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询通知', 'notification:list', 'api', '/notifications', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'notification:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '发送通知', 'notification:create', 'api', '/notifications', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'notification:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '标记通知已读',
       'notification:read',
       'api',
       '/notifications/{notificationId}/read',
       'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'notification:read');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '删除通知',
       'notification:delete',
       'api',
       '/notifications/admin/{notificationId}',
       'DELETE' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'notification:delete');

-- 文书管理
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询文书列表', 'document:list', 'api', '/office-documents', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'document:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '创建文书', 'document:create', 'api', '/office-documents', 'POST' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'document:create');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '更新文书',
       'document:update',
       'api',
       '/office-documents/{documentId}',
       'PATCH' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'document:update');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '导出文书',
       'document:export',
       'api',
       '/office-documents/{documentId}/export',
       'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'document:export');

-- 系统日志
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询操作日志', 'log:operation:list', 'api', '/operation-logs', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'log:operation:list');
INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询登录日志', 'log:login:list', 'api', '/login-logs', 'GET' WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'log:login:list');

-- 初始化角色
INSERT INTO `sys_role` (`name`, `code`, `data_scope`, `description`)
SELECT '系统管理员',
       'super_admin',
       'all',
       '拥有系统全部接口权限' WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `code` = 'super_admin');

INSERT INTO `sys_role` (`name`, `code`, `data_scope`, `description`)
SELECT '正式成员',
       'formal_member',
       'self',
       '社团正式成员基础角色' WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `code` = 'formal_member');

INSERT INTO `sys_role` (`name`, `code`, `data_scope`, `description`)
SELECT '非正式成员',
       'probationary_member',
       'self',
       '社团非正式成员基础角色' WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `code` = 'probationary_member');

-- 初始化管理员账号，密码为 admin123456 的 SHA-256
INSERT INTO `tb_user` (`user_name`, `password`, `real_name`, `student_id`, `user_status`)
SELECT 'admin',
       'ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78',
       '系统管理员',
       'admin',
       1 WHERE NOT EXISTS (SELECT 1 FROM `tb_user` WHERE `user_name` = 'admin' OR `student_id` = 'admin');

-- 初始化系统配置
INSERT INTO `system_setting` (`setting_key`, `setting_value`, `description`)
SELECT 'register_enabled',
       'false',
       '是否开放用户自助注册' WHERE NOT EXISTS (SELECT 1 FROM `system_setting` WHERE `setting_key` = 'register_enabled');

-- 绑定 super_admin 全部权限
INSERT
IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p
WHERE r.`code` = 'super_admin';

-- 绑定正式成员基础权限
INSERT
IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` IN ('auth:me', 'auth:logout', 'user:info', 'user:update')
WHERE r.`code` = 'formal_member';

-- 绑定非正式成员基础权限
INSERT
IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` IN ('auth:me', 'auth:logout', 'user:info')
WHERE r.`code` = 'probationary_member';

-- 绑定 admin 用户为 super_admin
INSERT
IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `tb_user` u
         JOIN `sys_role` r
WHERE u.`user_name` = 'admin'
  AND r.`code` = 'super_admin';

SET
FOREIGN_KEY_CHECKS = 1;
