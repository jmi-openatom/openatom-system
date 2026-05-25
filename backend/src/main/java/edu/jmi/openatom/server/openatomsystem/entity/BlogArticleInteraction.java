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
 * 博客互动记录
 *
 * <p>对应数据库表 blog_article_interaction, 记录用户点赞、收藏和分享文章的行为
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("blog_article_interaction")
public class BlogArticleInteraction {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("article_id")
  private Integer articleId;

  @TableField("user_id")
  private Integer userId;

  @TableField("interaction_type")
  private String interactionType;

  private String channel;

  @TableField("created_at")
  private Timestamp createdAt;
}
