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

/** docx 文档模板。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_template")
public class DocumentTemplate {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("template_type")
  private String templateType;

  @TableField("template_name")
  private String templateName;

  private Integer version;

  @TableField("file_path")
  private String filePath;

  @TableField("original_file_name")
  private String originalFileName;

  private String variables;
  private String status;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;
}
