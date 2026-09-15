CREATE TABLE quest_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(64) NULL,
    avatar_url VARCHAR(512) NULL,
    email VARCHAR(191) NULL,
    school VARCHAR(128) NULL,
    college VARCHAR(128) NULL,
    major VARCHAR(128) NULL,
    grade VARCHAR(32) NULL,
    skills_json JSON NULL,
    code_profile_url VARCHAR(512) NULL,
    weekly_hours INT NULL,
    bio VARCHAR(1000) NULL,
    conduct_agreed_at DATETIME(3) NULL,
    profile_completed_at DATETIME(3) NULL,
    onboarding_completed_at DATETIME(3) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    current_level VARCHAR(16) NOT NULL DEFAULT 'L0',
    total_points INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT ck_quest_member_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_quest_member_weekly_hours CHECK (weekly_hours IS NULL OR weekly_hours BETWEEN 0 AND 168)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_oauth_identity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    subject VARCHAR(191) NOT NULL,
    display_name VARCHAR(128) NULL,
    avatar_url VARCHAR(512) NULL,
    email VARCHAR(191) NULL,
    first_login_at DATETIME(3) NOT NULL,
    last_login_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_oauth_provider_subject (provider, subject),
    KEY idx_quest_oauth_member (member_id),
    CONSTRAINT fk_quest_oauth_member FOREIGN KEY (member_id) REFERENCES quest_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_key VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    permission_key VARCHAR(96) NOT NULL,
    name VARCHAR(96) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_permission_key (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_member_role (
    member_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_by BIGINT NULL,
    assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (member_id, role_id),
    CONSTRAINT fk_quest_member_role_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_member_role_role FOREIGN KEY (role_id) REFERENCES quest_role (id),
    CONSTRAINT fk_quest_member_role_assigner FOREIGN KEY (assigned_by) REFERENCES quest_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_quest_role_permission_role FOREIGN KEY (role_id) REFERENCES quest_role (id),
    CONSTRAINT fk_quest_role_permission_permission FOREIGN KEY (permission_id) REFERENCES quest_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_technical_direction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    direction_key VARCHAR(64) NOT NULL,
    name VARCHAR(96) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_direction_key (direction_key),
    CONSTRAINT ck_quest_direction_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_member_direction (
    member_id BIGINT NOT NULL,
    direction_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (member_id, direction_id),
    CONSTRAINT fk_quest_member_direction_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_member_direction_direction FOREIGN KEY (direction_id) REFERENCES quest_technical_direction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_onboarding_progress (
    member_id BIGINT NOT NULL,
    current_step INT NOT NULL DEFAULT 1,
    completed_steps_json JSON NOT NULL,
    self_assessment_json JSON NULL,
    assessment_result_json JSON NULL,
    draft_json JSON NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (member_id),
    CONSTRAINT fk_quest_onboarding_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_onboarding_step CHECK (current_step BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_member_id BIGINT NULL,
    action VARCHAR(96) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NULL,
    result VARCHAR(24) NOT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    detail_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quest_audit_actor_time (actor_member_id, created_at),
    KEY idx_quest_audit_target (target_type, target_id),
    CONSTRAINT fk_quest_audit_actor FOREIGN KEY (actor_member_id) REFERENCES quest_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO quest_role (role_key, name, description, built_in) VALUES
    ('MEMBER', '新成员', '领取、提交任务并查看自己的成长记录', TRUE),
    ('MENTOR', '导师', '审核授权范围内的成员任务', TRUE),
    ('PROJECT_OWNER', '项目负责人', '维护负责的项目与真实项目任务', TRUE),
    ('ADMIN', '管理员', '管理 Quest 平台配置、权限和运营数据', TRUE);

INSERT INTO quest_permission (permission_key, name) VALUES
    ('task:read', '浏览任务'),
    ('task:claim', '领取任务'),
    ('task:submit', '提交成果'),
    ('submission:review', '审核成果'),
    ('task:manage', '管理任务'),
    ('route:manage', '管理成长路线'),
    ('member:manage', '管理成员'),
    ('role:manage', '管理角色'),
    ('stats:global', '查看全局统计'),
    ('audit:read', '查看操作日志');

INSERT INTO quest_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM quest_role r JOIN quest_permission p
WHERE r.role_key = 'MEMBER' AND p.permission_key IN ('task:read', 'task:claim', 'task:submit');

INSERT INTO quest_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM quest_role r JOIN quest_permission p
WHERE r.role_key = 'MENTOR' AND p.permission_key IN ('task:read', 'task:claim', 'task:submit', 'submission:review');

INSERT INTO quest_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM quest_role r JOIN quest_permission p
WHERE r.role_key = 'PROJECT_OWNER' AND p.permission_key IN ('task:read', 'task:claim', 'task:submit', 'submission:review', 'task:manage');

INSERT INTO quest_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM quest_role r CROSS JOIN quest_permission p
WHERE r.role_key = 'ADMIN';

INSERT INTO quest_technical_direction (direction_key, name, sort_order) VALUES
    ('openharmony', 'OpenHarmony / ArkTS', 10),
    ('frontend', '前端开发', 20),
    ('java-backend', 'Java 后端', 30),
    ('python-ai', 'Python 与人工智能', 40),
    ('ui-ux', 'UI/UX 设计', 50),
    ('product-community', '产品与社区运营', 60),
    ('technical-writing', '技术文档', 70),
    ('testing-qa', '测试与质量保障', 80);
