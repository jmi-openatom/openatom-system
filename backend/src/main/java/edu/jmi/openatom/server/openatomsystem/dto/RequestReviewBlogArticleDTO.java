package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.Data;

/**
 * 管理员审核/管控博客文章请求
 *
 * <p>支持通过审核、退回待审核、隐藏、驳回、恢复草稿以及设置推荐
 */
@Data
public class RequestReviewBlogArticleDTO {
  private String status;
  private String reason;
  private Boolean featured;
}
