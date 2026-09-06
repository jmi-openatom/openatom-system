package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMemberProfileCommentVO {
  private Long id;
  private Integer profileUserId;
  private Integer userId;
  private Long parentId;
  private Long rootId;
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
  private List<ResponseMemberProfileCommentVO> replies;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
