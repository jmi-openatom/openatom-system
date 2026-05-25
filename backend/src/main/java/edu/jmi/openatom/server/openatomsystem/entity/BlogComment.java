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
 * 博客评论
 *
 * <p>对应数据库表 blog_comment, 记录文章评论及管理员可见性状态
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("blog_comment")
public class BlogComment {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("article_id")
  private Integer articleId;

  @TableField("user_id")
  private Integer userId;

  private String content;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
