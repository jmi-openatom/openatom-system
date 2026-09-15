CREATE TABLE quest_level_rule (
    level_key VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    minimum_points INT NOT NULL,
    sort_order INT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (level_key),
    UNIQUE KEY uk_quest_level_minimum (minimum_points),
    CONSTRAINT ck_quest_level_points CHECK (minimum_points >= 0),
    CONSTRAINT ck_quest_level_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quest_member_route (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    current_stage_id BIGINT NULL,
    enrolled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quest_member_route (member_id, route_id),
    KEY idx_quest_member_route_status (member_id, status),
    CONSTRAINT fk_quest_member_route_member FOREIGN KEY (member_id) REFERENCES quest_member (id),
    CONSTRAINT fk_quest_member_route_route FOREIGN KEY (route_id) REFERENCES quest_growth_route (id),
    CONSTRAINT fk_quest_member_route_stage FOREIGN KEY (current_stage_id) REFERENCES quest_growth_stage (id),
    CONSTRAINT ck_quest_member_route_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO quest_level_rule (level_key, name, minimum_points, sort_order) VALUES
    ('L0', '新人', 0, 10),
    ('L1', '开源学习者', 100, 20),
    ('L2', '实践成员', 300, 30),
    ('L3', '项目贡献者', 700, 40),
    ('L4', '核心贡献者', 1500, 50),
    ('L5', '导师', 3000, 60);
