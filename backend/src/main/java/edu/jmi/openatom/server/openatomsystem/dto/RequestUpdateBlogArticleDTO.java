package edu.jmi.openatom.server.openatomsystem.dto;

import java.util.List;
import lombok.Data;

/**
 * 更新博客文章请求
 *
 * <p>作者维护自己的草稿、待审核文章、已发布文章或被驳回文章时使用
 */
@Data
public class RequestUpdateBlogArticleDTO {
  private String title;
  private String contentMarkdown;
  private String summary;
  private String coverUrl;
  private String category;
  private List<String> tags;
  private String status;
  private Boolean commentsEnabled;
}
