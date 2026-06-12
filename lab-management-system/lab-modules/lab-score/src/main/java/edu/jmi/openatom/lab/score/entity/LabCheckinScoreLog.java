package edu.jmi.openatom.lab.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_checkin_score_log")
public class LabCheckinScoreLog {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private LocalDate checkinDate;
  private Integer attendanceStatus;
  private String source;
  private LocalDateTime checkinAt;
  private Integer localScoreChange;
  private Integer clubScoreChange;
  private Integer clubSyncStatus;
  private String syncError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
