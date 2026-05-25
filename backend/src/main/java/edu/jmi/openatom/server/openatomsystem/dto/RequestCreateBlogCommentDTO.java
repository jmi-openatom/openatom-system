package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建博客评论请求
 *
 * <p>登录用户在公开文章下发表评论时使用
 */
@Data
public class RequestCreateBlogCommentDTO {
  @NotBlank(message = "评论内容不能为空")
  private String content;
}
