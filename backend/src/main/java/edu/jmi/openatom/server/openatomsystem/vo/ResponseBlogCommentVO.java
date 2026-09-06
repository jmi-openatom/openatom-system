package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
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
  private Integer parentId;
  private Integer rootId;
  private Integer replyToUserId;
  private String replyToUserName;
  private Boolean replyTargetHidden;
  private String userName;
  private String userAvatar;
  private String content;
  private String status;
  private Integer replyCount;
  private Integer likeCount;
  private Boolean liked;
  private Boolean own;
  private Boolean author;
  private Integer reportCount;
  private List<String> reportReasons;
  private List<ResponseBlogCommentVO> replies;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
