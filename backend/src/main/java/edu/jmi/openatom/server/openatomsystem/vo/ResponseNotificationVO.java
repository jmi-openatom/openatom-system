package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息VO
 *
 * <p>包含通知标题, 内容, 类型, 创建时间及已读状态, 用于系统消息推送与展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseNotificationVO {
  private Integer id;
  private String title;
  private String content;
  private String type;
  private Timestamp createdAt;
  private Integer readFlag;
  private Timestamp readAt;
}
