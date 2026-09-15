CREATE TABLE quest_growth_route (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NULL,
    direction_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    published_at DATETIME(3) NULL,
    archived_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_route_key (route_key),
    KEY idx_quest_route_direction_status (direction_id, status),
    CONSTRAINT fk_quest_route_direction FOREIGN KEY (direction_id) REFERENCES quest_technical_direction (id),
    CONSTRAINT fk_quest_route_creator FOREIGN KEY (created_by) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_route_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_growth_stage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stage_key VARCHAR(32) NOT NULL,
    name VARCHAR(96) NOT NULL,
    objective VARCHAR(1000) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_stage_key (stage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_route_stage (
    route_id BIGINT NOT NULL,
    stage_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    required_task_count INT NOT NULL DEFAULT 0,
    optional_task_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (route_id, stage_id),
    UNIQUE KEY uk_quest_route_stage_order (route_id, sort_order),
    CONSTRAINT fk_quest_route_stage_route FOREIGN KEY (route_id) REFERENCES quest_growth_route (id),
    CONSTRAINT fk_quest_route_stage_stage FOREIGN KEY (stage_id) REFERENCES quest_growth_stage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_key VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    direction_id BIGINT NULL,
    route_id BIGINT NULL,
    stage_id BIGINT NULL,
    task_type VARCHAR(32) NOT NULL,
    difficulty VARCHAR(24) NOT NULL,
    learning_objectives_json JSON NOT NULL,
    estimated_minutes INT NOT NULL,
    deadline_type VARCHAR(24) NOT NULL DEFAULT 'NONE',
    fixed_deadline DATETIME(3) NULL,
    duration_hours INT NULL,
    instructions TEXT NOT NULL,
    resources_json JSON NOT NULL,
    submission_requirements TEXT NOT NULL,
    acceptance_criteria TEXT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    owner_member_id BIGINT NOT NULL,
    faq_json JSON NOT NULL,
    submission_limit INT NULL,
    capacity INT NULL,
    assignment_mode VARCHAR(24) NOT NULL DEFAULT 'INDIVIDUAL',
    required_in_stage BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(3) NULL,
    archived_at DATETIME(3) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_task_key (task_key),
    KEY idx_quest_task_catalog (status, direction_id, stage_id),
    KEY idx_quest_task_owner (owner_member_id),
    CONSTRAINT fk_quest_task_direction FOREIGN KEY (direction_id) REFERENCES quest_technical_direction (id),
    CONSTRAINT fk_quest_task_route FOREIGN KEY (route_id) REFERENCES quest_growth_route (id),
    CONSTRAINT fk_quest_task_stage FOREIGN KEY (stage_id) REFERENCES quest_growth_stage (id),
    CONSTRAINT fk_quest_task_owner FOREIGN KEY (owner_member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_task_creator FOREIGN KEY (created_by) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_task_type CHECK (task_type IN ('ONBOARDING', 'LEARNING', 'CHALLENGE', 'COLLABORATION', 'REAL_PROJECT', 'LIMITED_EVENT')),
    CONSTRAINT ck_quest_task_difficulty CHECK (difficulty IN ('ENTRY', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_quest_task_deadline_type CHECK (deadline_type IN ('NONE', 'FIXED', 'AFTER_CLAIM')),
    CONSTRAINT ck_quest_task_assignment_mode CHECK (assignment_mode IN ('INDIVIDUAL', 'TEAM')),
    CONSTRAINT ck_quest_task_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE', 'ARCHIVED')),
    CONSTRAINT ck_quest_task_points CHECK (points >= 0),
    CONSTRAINT ck_quest_task_estimated_minutes CHECK (estimated_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_task_prerequisite (
    task_id BIGINT NOT NULL,
    prerequisite_task_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, prerequisite_task_id),
    CONSTRAINT fk_quest_task_prerequisite_task FOREIGN KEY (task_id) REFERENCES quest_task (id),
    CONSTRAINT fk_quest_task_prerequisite_required FOREIGN KEY (prerequisite_task_id) REFERENCES quest_task (id),
    CONSTRAINT ck_quest_task_not_self_prerequisite CHECK (task_id <> prerequisite_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_task_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    assigned_by BIGINT NULL,
    source VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'IN_PROGRESS',
    claimed_at DATETIME(3) NOT NULL,
    due_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    abandoned_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_assignment_member_task (member_id, task_id),
    KEY idx_quest_assignment_task_status (task_id, status),
    KEY idx_quest_assignment_member_status (member_id, status),
    CONSTRAINT fk_quest_assignment_task FOREIGN KEY (task_id) REFERENCES quest_task (id),
    CONSTRAINT fk_quest_assignment_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_assignment_assigner FOREIGN KEY (assigned_by) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_assignment_source CHECK (source IN ('CLAIMED', 'ASSIGNED')),
    CONSTRAINT ck_quest_assignment_status CHECK (status IN ('IN_PROGRESS', 'PENDING_REVIEW', 'REVISION_REQUIRED', 'PASSED', 'OVERDUE', 'ABANDONED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_task_submission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    completion_note TEXT NOT NULL,
    repository_url VARCHAR(512) NULL,
    pull_request_url VARCHAR(512) NULL,
    demo_url VARCHAR(512) NULL,
    video_url VARCHAR(512) NULL,
    problems_and_learning TEXT NULL,
    ai_used BOOLEAN NOT NULL DEFAULT FALSE,
    ai_usage_detail TEXT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_submission_assignment_version (assignment_id, version_no),
    KEY idx_quest_submission_status_time (status, submitted_at),
    CONSTRAINT fk_quest_submission_assignment FOREIGN KEY (assignment_id) REFERENCES quest_task_assignment (id),
    CONSTRAINT ck_quest_submission_version CHECK (version_no > 0),
    CONSTRAINT ck_quest_submission_ai_detail CHECK (ai_used = FALSE OR ai_usage_detail IS NOT NULL),
    CONSTRAINT ck_quest_submission_status CHECK (status IN ('SUBMITTED', 'REVIEWING', 'REVISION_REQUIRED', 'PASSED', 'FAILED', 'SUPERSEDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_submission_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    scan_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quest_attachment_submission (submission_id),
    CONSTRAINT fk_quest_attachment_submission FOREIGN KEY (submission_id) REFERENCES quest_task_submission (id),
    CONSTRAINT ck_quest_attachment_size CHECK (size_bytes > 0),
    CONSTRAINT ck_quest_attachment_scan_status CHECK (scan_status IN ('PENDING', 'SAFE', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_review_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    reviewer_member_id BIGINT NOT NULL,
    result VARCHAR(32) NOT NULL,
    comment TEXT NOT NULL,
    score_json JSON NULL,
    required_changes_json JSON NULL,
    suggestions_json JSON NULL,
    strengths_json JSON NULL,
    resubmission_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    transferred_to BIGINT NULL,
    excellent BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quest_review_submission_time (submission_id, reviewed_at),
    KEY idx_quest_review_reviewer_time (reviewer_member_id, reviewed_at),
    CONSTRAINT fk_quest_review_submission FOREIGN KEY (submission_id) REFERENCES quest_task_submission (id),
    CONSTRAINT fk_quest_review_reviewer FOREIGN KEY (reviewer_member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_review_transfer FOREIGN KEY (transferred_to) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_review_result CHECK (result IN ('PASSED', 'REVISION_REQUIRED', 'FAILED', 'TRANSFERRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_review_appeal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    resolved_by BIGINT NULL,
    resolution TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_appeal_review_member (review_id, member_id),
    CONSTRAINT fk_quest_appeal_review FOREIGN KEY (review_id) REFERENCES quest_review_record (id),
    CONSTRAINT fk_quest_appeal_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_appeal_resolver FOREIGN KEY (resolved_by) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_appeal_status CHECK (status IN ('PENDING', 'UPHELD', 'OVERTURNED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_point_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    source_type VARCHAR(48) NOT NULL,
    source_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(191) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    adjusted_by BIGINT NULL,
    reversal_of BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_point_idempotency (idempotency_key),
    KEY idx_quest_point_member_time (member_id, created_at),
    CONSTRAINT fk_quest_point_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_point_adjuster FOREIGN KEY (adjusted_by) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_point_reversal FOREIGN KEY (reversal_of) REFERENCES quest_point_ledger (id),
    CONSTRAINT ck_quest_point_nonzero CHECK (amount <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    notification_type VARCHAR(48) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    action_url VARCHAR(512) NULL,
    business_type VARCHAR(64) NULL,
    business_id VARCHAR(128) NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quest_notification_member_read (member_id, read_at, created_at),
    CONSTRAINT fk_quest_notification_member FOREIGN KEY (member_id) REFERENCES quest_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_announcement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    published_by BIGINT NULL,
    published_at DATETIME(3) NULL,
    expires_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quest_announcement_status_time (status, published_at),
    CONSTRAINT fk_quest_announcement_publisher FOREIGN KEY (published_by) REFERENCES quest_member (id),
    CONSTRAINT ck_quest_announcement_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO quest_growth_stage (stage_key, name, objective) VALUES
    ('L0', '新人报到', '了解社团、平台和基础协作规则'),
    ('L1', '开源入门', '掌握 Git、代码托管平台与 Issue 协作基础'),
    ('L2', '技术学习', '掌握所选技术方向的基础技能'),
    ('L3', '项目实战', '完成接近真实开源项目的实践任务'),
    ('L4', '项目贡献', '参与真实开源项目并形成可验证贡献');
