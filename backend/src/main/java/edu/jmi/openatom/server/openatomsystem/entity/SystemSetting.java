package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统设置
 *
 * <p>对应数据库表 system_setting, 存储系统的键值对配置信息, 支持动态调整系统参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("system_setting")
public class SystemSetting {
  @TableId("setting_key")
  private String settingKey;

  @TableField("setting_value")
  private String settingValue;

  private String description;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
