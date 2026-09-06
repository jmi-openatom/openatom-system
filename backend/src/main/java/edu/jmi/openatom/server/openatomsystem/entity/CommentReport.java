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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment_report")
public class CommentReport {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  @TableField("target_type") private String targetType;
  @TableField("comment_id") private Long commentId;
  @TableField("reporter_user_id") private Integer reporterUserId;
  private String reason;
  private String status;
  @TableField("created_at") private Timestamp createdAt;
  @TableField("updated_at") private Timestamp updatedAt;
}
