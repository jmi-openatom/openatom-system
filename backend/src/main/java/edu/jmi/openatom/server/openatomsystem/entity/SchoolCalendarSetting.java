package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 校历基础设置 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("school_calendar_setting")
public class SchoolCalendarSetting {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("start_date")
  private Date startDate;

  @TableField("week_count")
  private Integer weekCount;

  @TableField("updated_by")
  private Integer updatedBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
