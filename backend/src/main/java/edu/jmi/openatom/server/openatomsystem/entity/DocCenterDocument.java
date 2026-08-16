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

/** 文档中心：社团成员上传的 Office 文件（docx/xlsx/pptx），经 ONLYOFFICE 在线编辑。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("doc_center_document")
public class DocCenterDocument {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("owner_user_id")
  private Integer ownerUserId;

  @TableField("name")
  private String name;

  @TableField("extension")
  private String extension;

  @TableField("size_bytes")
  private Long sizeBytes;

  @TableField("storage_name")
  private String storageName;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
