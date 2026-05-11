package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息DTO
 *
 * <p>包含通知标题, 内容, 类型, 创建时间及已读状态, 用于系统消息推送与展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseNotificationDTO {
  private Integer id;
  private String title;
  private String content;
  private String type;
  private Timestamp createdAt;
  private Integer readFlag;
  private Timestamp readAt;
}
