package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 成员主页评论后台管理视图。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAdminMemberProfileCommentVO {
  private Long id;
  private Integer profileUserId;
  private String profileName;
  private String profileSlug;
  private Integer userId;
  private String userName;
  private String userAvatar;
  private Long parentId;
  private Long rootId;
  private String replyToUserName;
  private Integer reportCount;
  private List<String> reportReasons;
  private String content;
  private String status;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
