package edu.jmi.openatom.server.openatomsystem.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认成员关系初始化器
 *
 * <p>应用启动时将系统中所有用户同步为默认社团的试用期成员
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class DefaultMembershipInitializer implements ApplicationRunner {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    Integer clubId =
        jdbcTemplate.query(
            "SELECT id FROM club WHERE code = ? LIMIT 1",
            resultSet -> resultSet.next() ? resultSet.getInt("id") : null,
            DEFAULT_CLUB_CODE);
    if (clubId == null) {
      return;
    }
    int rows =
        jdbcTemplate.update(
            """
            INSERT INTO club_membership (`user_id`, `club_id`, `status`, `featured`, `sort_order`)
            SELECT u.`id`, ?, 'probation', 0, 0
            FROM tb_user u
            WHERE NOT EXISTS (
              SELECT 1
              FROM club_membership m
              WHERE m.`user_id` = u.`id`
                AND m.`club_id` = ?
                AND m.`left_at` IS NULL
            )
            """,
            clubId,
            clubId);
    if (rows > 0) {
      log.info("Synchronized default club memberships: {} users", rows);
    }
  }
}
