package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class DefaultClubContentInitializer implements ApplicationRunner {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final JdbcTemplate jdbcTemplate;
  private final ClubMapper clubMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    createTables();
    Club club = clubMapper.selectOne(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Club>()
            .eq(Club::getCode, DEFAULT_CLUB_CODE)
            .last("LIMIT 1"));
    if (club == null) {
      return;
    }
    seedActivities(club.getId());
    seedAwards(club.getId());
    seedRecruitmentCampaign(club.getId());
  }

  private void createTables() {
    addColumnIfAbsent("club_membership", "featured", "TINYINT(1) DEFAULT 0 COMMENT '是否在官网主要人员展示'");
    addColumnIfAbsent("club_membership", "sort_order", "INT DEFAULT 0 COMMENT '官网展示排序'");
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `club_activity`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `title` VARCHAR(120) NOT NULL COMMENT '活动标题',
            `summary` VARCHAR(500) DEFAULT NULL COMMENT '活动摘要',
            `description_markdown` MEDIUMTEXT DEFAULT NULL COMMENT 'Markdown 活动介绍',
            `activity_at` TIMESTAMP NULL DEFAULT NULL COMMENT '活动开始时间',
            `end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '活动结束时间',
            `location` VARCHAR(120) DEFAULT NULL COMMENT '活动地点',
            `status` VARCHAR(30) DEFAULT 'draft' COMMENT '状态',
            `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
            `registration_required` TINYINT(1) DEFAULT 0 COMMENT '是否需要官网报名',
            `registration_start_at` TIMESTAMP NULL DEFAULT NULL COMMENT '报名开始时间',
            `registration_end_at` TIMESTAMP NULL DEFAULT NULL COMMENT '报名结束时间',
            `registration_fields` JSON DEFAULT NULL COMMENT '自定义报名字段',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_activity_club_time` (`club_id`, `activity_at`),
            KEY `idx_activity_status` (`status`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='社团活动表'
        """);
    addColumnIfAbsent("club_activity", "summary", "VARCHAR(500) DEFAULT NULL COMMENT '活动摘要'");
    addColumnIfAbsent("club_activity", "description_markdown", "MEDIUMTEXT DEFAULT NULL COMMENT 'Markdown 活动介绍'");
    addColumnIfAbsent("club_activity", "end_at", "TIMESTAMP NULL DEFAULT NULL COMMENT '活动结束时间'");
    addColumnIfAbsent("club_activity", "cover_url", "VARCHAR(255) DEFAULT NULL COMMENT '封面URL'");
    addColumnIfAbsent("club_activity", "registration_required", "TINYINT(1) DEFAULT 0 COMMENT '是否需要官网报名'");
    addColumnIfAbsent("club_activity", "registration_start_at", "TIMESTAMP NULL DEFAULT NULL COMMENT '报名开始时间'");
    addColumnIfAbsent("club_activity", "registration_end_at", "TIMESTAMP NULL DEFAULT NULL COMMENT '报名结束时间'");
    addColumnIfAbsent("club_activity", "registration_fields", "JSON DEFAULT NULL COMMENT '自定义报名字段'");
    addColumnIfAbsent("club_activity", "updated_at", "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
    addColumnIfAbsent(
        "recruitment_campaign",
        "login_required",
        "TINYINT(1) DEFAULT 1 COMMENT '是否要求登录后提交'");
    addColumnIfAbsent(
        "recruitment_campaign",
        "form_schema",
        "JSON DEFAULT NULL COMMENT '自定义报名表单结构'");
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `activity_registration`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `activity_id` INT NOT NULL COMMENT '活动ID',
            `user_id` INT NOT NULL COMMENT '报名用户ID',
            `form_data` JSON DEFAULT NULL COMMENT '报名表单内容',
            `status` VARCHAR(30) DEFAULT 'submitted' COMMENT '状态',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `uk_activity_registration_user` (`activity_id`, `user_id`),
            KEY `idx_activity_registration_activity` (`activity_id`),
            KEY `idx_activity_registration_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='活动报名表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `club_award`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `title` VARCHAR(120) NOT NULL COMMENT '获奖标题',
            `competition_name` VARCHAR(160) NOT NULL COMMENT '比赛名称',
            `award_level` VARCHAR(80) DEFAULT NULL COMMENT '奖项等级',
            `award_year` INT DEFAULT NULL COMMENT '获奖年份',
            `team_name` VARCHAR(120) DEFAULT NULL COMMENT '队伍/成员',
            `description` VARCHAR(500) DEFAULT NULL COMMENT '获奖说明',
            `sort_order` INT DEFAULT 0 COMMENT '排序',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            PRIMARY KEY (`id`),
            KEY `idx_award_club_year` (`club_id`, `award_year`),
            KEY `idx_award_sort_order` (`sort_order`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='社团比赛获奖表'
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `form_submission`
        (
            `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `form_id` INT NOT NULL COMMENT '表单ID',
            `club_id` INT NOT NULL COMMENT '社团ID',
            `user_id` INT NULL COMMENT '提交用户ID',
            `anonymous_name` VARCHAR(80) DEFAULT NULL COMMENT '匿名提交联系人',
            `anonymous_contact` VARCHAR(120) DEFAULT NULL COMMENT '匿名联系方式',
            `form_data` JSON DEFAULT NULL COMMENT '表单数据',
            `status` VARCHAR(30) DEFAULT 'submitted' COMMENT '提交状态',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id`),
            KEY `idx_form_submission_form` (`form_id`),
            KEY `idx_form_submission_club` (`club_id`),
            KEY `idx_form_submission_user` (`user_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通用表单提交记录表'
        """);
    jdbcTemplate.execute(
        """
        ALTER TABLE `membership_application`
        MODIFY COLUMN `user_id` INT NULL COMMENT '申请用户ID，匿名提交时可为空'
        """);
  }

  private void seedActivities(Integer clubId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM club_activity WHERE club_id = ?", Integer.class, clubId);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.update(
        """
        INSERT INTO club_activity
        (`club_id`, `title`, `summary`, `description_markdown`, `activity_at`, `end_at`, `location`, `status`, `registration_required`, `registration_start_at`, `registration_end_at`, `registration_fields`)
        VALUES
        (?, 'Web 全栈项目冲刺夜', '完成社团内部工具的接口联调、页面验收与部署复盘。', '# Web 全栈项目冲刺夜\n\n本次活动聚焦接口联调、页面验收和部署复盘。\n\n## 你会完成\n\n- 拆解一个真实后台模块\n- 完成前后端联调\n- 输出可复用的验收清单', '2026-05-12 19:00:00', '2026-05-12 21:30:00', '实验楼 A305', 'published', 1, '2026-05-05 09:00:00', '2026-05-12 12:00:00', '[{\"label\":\"技术方向\",\"type\":\"select\",\"required\":true,\"options\":[\"前端\",\"后端\",\"测试\",\"产品\"]},{\"label\":\"想解决的问题\",\"type\":\"textarea\",\"required\":false}]'),
        (?, '蓝桥杯赛后题解分享', '由参赛队员拆解典型题目，沉淀可复用的解题模板。', '# 蓝桥杯赛后题解分享\n\n我们会复盘典型题、复杂度分析和代码模板。', '2026-05-18 19:00:00', '2026-05-18 21:00:00', '线上会议', 'published', 0, NULL, NULL, NULL),
        (?, '开源协作工作坊', '从 Issue 拆分、分支协作到 Pull Request Review 进行实战训练。', '# 开源协作工作坊\n\n从 Issue 拆分、分支协作到 Pull Request Review 进行实战训练。', '2026-05-25 19:00:00', '2026-05-25 21:30:00', '实验楼 B210', 'published', 1, '2026-05-10 09:00:00', '2026-05-24 22:00:00', '[{\"label\":\"GitHub 主页\",\"type\":\"text\",\"required\":false},{\"label\":\"是否有 PR 经验\",\"type\":\"radio\",\"required\":true,\"options\":[\"有\",\"没有\"]}]')
        """,
        clubId,
        clubId,
        clubId);
    log.info("Initialized default club activities: clubId={}", clubId);
  }

  private void seedAwards(Integer clubId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM club_award WHERE club_id = ?", Integer.class, clubId);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.update(
        """
        INSERT INTO club_award
        (`club_id`, `title`, `competition_name`, `award_level`, `award_year`, `team_name`, `description`, `sort_order`)
        VALUES
        (?, '省级一等奖', '蓝桥杯软件赛 Java 组', '一等奖', 2026, '算法训练队', '2 名成员进入省级一等奖名单。', 10),
        (?, '华东赛区银奖', '中国高校计算机大赛团体程序设计天梯赛', '银奖', 2025, 'JMI-OpenAtom', '团队程序设计方向突破赛区奖项。', 20),
        (?, '省级二等奖', '服务外包创新创业大赛', '二等奖', 2025, '校园数字化项目组', '围绕校园协作工具完成方案设计和原型交付。', 30),
        (?, '校级优秀开源项目', '校园数字化创新实践评比', '最佳工程实践', 2024, 'OpenAtom 工程组', '以工程质量、文档和协作流程获得评审认可。', 40)
        """,
        clubId,
        clubId,
        clubId,
        clubId);
    log.info("Initialized default club awards: clubId={}", clubId);
  }

  private void seedRecruitmentCampaign(Integer clubId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM recruitment_campaign WHERE club_id = ? AND status IN ('open', 'published')",
            Integer.class,
            clubId);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.update(
        """
        INSERT INTO recruitment_campaign
        (`club_id`, `name`, `apply_start_at`, `apply_end_at`, `login_required`, `form_schema`, `status`)
        VALUES
        (?, '2026 社团信息收集', CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY), 0, '[{"key":"name","label":"姓名","type":"text","required":true,"placeholder":"请输入姓名"},{"key":"studentId","label":"学号","type":"text","required":true,"placeholder":"请输入学号"},{"key":"direction","label":"感兴趣方向","type":"select","required":true,"options":[{"label":"前端","value":"frontend"},{"label":"后端","value":"backend"},{"label":"测试","value":"qa"},{"label":"产品","value":"product"}]},{"key":"intro","label":"自我介绍","type":"textarea","required":false,"placeholder":"可填写个人背景、经历或兴趣点"}]', 'open')
        """,
        clubId);
    log.info("Initialized default recruitment campaign: clubId={}", clubId);
  }

  private void addColumnIfAbsent(String tableName, String columnName, String definition) {
    Integer tableCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
            """,
            Integer.class,
            tableName);
    if (tableCount == null || tableCount == 0) {
      return;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
            """,
            Integer.class,
            tableName,
            columnName);
    if (count == null || count > 0) {
      return;
    }
    jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + definition);
  }
}
