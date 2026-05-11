package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知接收记录
 *
 * <p>对应数据库表 notification_receiver, 记录通知的接收用户及其阅读状态
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("notification_receiver")
public class NotificationReceiver {
  @TableField("notification_id")
  private Integer notificationId;

  @TableField("receiver_user_id")
  private Integer receiverUserId;

  @TableField("read_flag")
  private Integer readFlag;

  @TableField("read_at")
  private Timestamp readAt;
}
