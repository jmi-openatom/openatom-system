package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
