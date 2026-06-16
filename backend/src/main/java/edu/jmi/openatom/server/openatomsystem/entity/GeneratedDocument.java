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

/** 基于模板生成的 docx 文档。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("generated_document")
public class GeneratedDocument {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("session_id")
  private Long sessionId;

  @TableField("plan_id")
  private Long planId;

  @TableField("template_id")
  private Long templateId;

  @TableField("document_type")
  private String documentType;

  @TableField("file_name")
  private String fileName;

  @TableField("file_path")
  private String filePath;

  @TableField("filled_variables")
  private String filledVariables;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;
}
