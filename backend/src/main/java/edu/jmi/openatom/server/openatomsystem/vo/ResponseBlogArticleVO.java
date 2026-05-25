package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客文章响应
 *
 * <p>包含文章内容、作者信息、管控状态和互动统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBlogArticleVO {
  private Integer id;
  private Integer clubId;
  private Integer authorId;
  private String authorName;
  private String authorAvatar;
  private String title;
  private String summary;
  private String contentMarkdown;
  private String coverUrl;
  private String category;
  private List<String> tags;
  private String status;
  private Boolean featured;
  private Integer viewCount;
  private Integer likeCount;
  private Integer favoriteCount;
  private Integer shareCount;
  private Long commentCount;
  private Boolean liked;
  private Boolean favorited;
  private String rejectReason;
  private Integer reviewedBy;
  private String reviewerName;
  private Timestamp reviewedAt;
  private Timestamp publishedAt;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
