package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客评论响应
 *
 * <p>包含评论内容、评论人基础信息和状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBlogCommentVO {
  private Integer id;
  private Integer articleId;
  private Integer userId;
  private String userName;
  private String userAvatar;
  private String content;
  private String status;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
