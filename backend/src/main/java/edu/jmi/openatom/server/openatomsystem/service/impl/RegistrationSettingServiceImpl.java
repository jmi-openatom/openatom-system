package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.service.RegistrationSettingService;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RegistrationSettingServiceImpl implements RegistrationSettingService {
  private static final String REGISTER_ENABLED_KEY = "register_enabled";

  private final JdbcTemplate jdbcTemplate;

  public boolean isRegisterEnabled() {
    ensureSettingRow();
    String value =
        jdbcTemplate.query(
            "SELECT setting_value FROM system_setting WHERE setting_key = ? LIMIT 1",
            resultSet -> resultSet.next() ? resultSet.getString("setting_value") : null,
            REGISTER_ENABLED_KEY);
    return Boolean.parseBoolean(value);
  }

  public boolean updateRegisterEnabled(boolean enabled) {
    ensureSettingRow();
    jdbcTemplate.update(
        "UPDATE system_setting SET setting_value = ? WHERE setting_key = ?",
        String.valueOf(enabled),
        REGISTER_ENABLED_KEY);
    return enabled;
  }

  private void ensureSettingRow() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS `system_setting`
        (
            `setting_key` VARCHAR(100) NOT NULL COMMENT '配置键',
            `setting_value` VARCHAR(255) DEFAULT NULL COMMENT '配置值',
            `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
            `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`setting_key`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表'
        """);
    jdbcTemplate.update(
        """
        INSERT INTO system_setting (`setting_key`, `setting_value`, `description`)
        SELECT ?, 'false', '是否开放用户自助注册'
        WHERE NOT EXISTS (
            SELECT 1 FROM system_setting WHERE setting_key = ?
        )
        """,
        REGISTER_ENABLED_KEY,
        REGISTER_ENABLED_KEY);
  }
}
