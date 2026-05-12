package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 签到记录 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("checkin_record")
public class CheckInRecord {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("session_id")
  private Integer sessionId;

  @TableField("user_id")
  private Integer userId;

  @TableField("checkin_at")
  private Timestamp checkinAt;

  private String source;
  private String status;
}
