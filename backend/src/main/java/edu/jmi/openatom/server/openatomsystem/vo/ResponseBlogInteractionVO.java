package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客互动记录响应
 *
 * <p>用于后台查看点赞、收藏和分享明细
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBlogInteractionVO {
  private Integer id;
  private Integer articleId;
  private String articleTitle;
  private Integer userId;
  private String userName;
  private String userAvatar;
  private String interactionType;
  private String channel;
  private Timestamp createdAt;
}
