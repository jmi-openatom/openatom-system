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

/** 社团规章制度 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("club_regulation")
public class ClubRegulation {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String title;
  private String summary;

  @TableField("content_markdown")
  private String contentMarkdown;

  private String status;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("updated_by")
  private Integer updatedBy;

  @TableField("published_at")
  private Timestamp publishedAt;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
