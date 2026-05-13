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

/** 校历调休设置 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("school_calendar_adjustment")
public class SchoolCalendarAdjustment {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("calendar_date")
  private Date calendarDate;

  private String type;
  private String reason;

  @TableField("updated_by")
  private Integer updatedBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
