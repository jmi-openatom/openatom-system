package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客文章
 *
 * <p>对应数据库表 blog_article, 存储正式成员发布的技术文章及管理员管控状态
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("blog_article")
public class BlogArticle {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("author_id")
  private Integer authorId;

  private String title;
  private String summary;

  @TableField("content_markdown")
  private String contentMarkdown;

  @TableField("cover_url")
  private String coverUrl;

  private String category;
  private String tags;
  private String status;
  private Boolean featured;

  @TableField("view_count")
  private Integer viewCount;

  @TableField("like_count")
  private Integer likeCount;

  @TableField("reject_reason")
  private String rejectReason;

  @TableField("reviewed_by")
  private Integer reviewedBy;

  @TableField("reviewed_at")
  private Timestamp reviewedAt;

  @TableField("published_at")
  private Timestamp publishedAt;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
