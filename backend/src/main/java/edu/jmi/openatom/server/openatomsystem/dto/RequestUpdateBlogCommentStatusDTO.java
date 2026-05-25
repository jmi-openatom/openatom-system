package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新博客评论状态请求
 *
 * <p>管理员隐藏或恢复评论时使用
 */
@Data
public class RequestUpdateBlogCommentStatusDTO {
  @NotBlank(message = "评论状态不能为空")
  private String status;
}
