package edu.jmi.openatom.lab.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_notifications")
public class LabNotification {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String noticeType;
  private String title;
  private String content;
  private LocalDateTime readAt;
  private LocalDateTime createdAt;
}
