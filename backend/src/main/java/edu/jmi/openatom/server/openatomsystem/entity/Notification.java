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

/**
 * 通知
 *
 * <p>对应数据库表 notification, 存储系统发送的通知消息, 包含标题, 内容和类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("notification")
public class Notification {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String title;
  private String content;
  private String type;

  @TableField("created_at")
  private Timestamp createdAt;
}
