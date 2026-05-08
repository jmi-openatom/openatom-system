package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jmi.openatom.server.openatomsystem.entity.SystemSetting;
import edu.jmi.openatom.server.openatomsystem.mapper.SystemSettingMapper;
import edu.jmi.openatom.server.openatomsystem.service.RegistrationSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationSettingServiceImpl extends ServiceImpl<SystemSettingMapper, SystemSetting>
    implements RegistrationSettingService {
  private static final String REGISTER_ENABLED_KEY = "register_enabled";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public boolean isRegisterEnabled() {
    ensureSettingRow();
    SystemSetting setting = getById(REGISTER_ENABLED_KEY);
    return setting != null && Boolean.parseBoolean(setting.getSettingValue());
  }

  @Override
  public boolean updateRegisterEnabled(boolean enabled) {
    ensureSettingRow();
    SystemSetting setting =
        SystemSetting.builder()
            .settingKey(REGISTER_ENABLED_KEY)
            .settingValue(String.valueOf(enabled))
            .build();
    saveOrUpdate(setting);
    return enabled;
  }

  private void ensureSettingRow() {
    // Keep DDL in JdbcTemplate as MyBatis-Plus doesn't handle it
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

    if (getById(REGISTER_ENABLED_KEY) == null) {
      SystemSetting defaultSetting =
          SystemSetting.builder()
              .settingKey(REGISTER_ENABLED_KEY)
              .settingValue("false")
              .description("是否开放用户自助注册")
              .build();
      save(defaultSetting);
    }
  }
}
