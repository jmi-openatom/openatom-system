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
 * 公文实体
 *
 * <p>对应数据库表 office_document, 存储社团公文信息, 包括公文类型, 标题, 内容和审批状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("office_document")
public class OfficeDocument {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("doc_type")
  private String docType;

  private String title;

  private String status;

  private String payload;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("updated_by")
  private Integer updatedBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
