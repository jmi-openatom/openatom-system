package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestCreateMemberProfileCommentDTO {
  @NotBlank(message = "评论内容不能为空")
  private String content;

  private Long parentId;
}
