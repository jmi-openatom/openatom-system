package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/**
 * 创建博客文章请求
 *
 * <p>正式成员创建草稿或提交文章审核时使用
 */
@Data
public class RequestCreateBlogArticleDTO {
  @NotBlank(message = "文章标题不能为空")
  private String title;

  @NotBlank(message = "文章正文不能为空")
  private String contentMarkdown;

  private String summary;
  private String coverUrl;
  private String category;
  private List<String> tags;
  private String status;
}
