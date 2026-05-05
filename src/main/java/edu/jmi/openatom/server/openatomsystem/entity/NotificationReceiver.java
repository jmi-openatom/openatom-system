package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
