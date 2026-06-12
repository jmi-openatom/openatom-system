package edu.jmi.openatom.lab.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_checkin_record")
public class LabCheckInRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long sessionId;
  private Long userId;
  private LocalDateTime checkinAt;
  private String source;
  private String status;
}
