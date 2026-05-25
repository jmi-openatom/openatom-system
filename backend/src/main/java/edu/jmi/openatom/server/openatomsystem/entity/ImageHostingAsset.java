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
 * 图床资产
 *
 * <p>对应数据库表 image_hosting_asset, 保存上传图片的可管理记录
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("image_hosting_asset")
public class ImageHostingAsset {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("uploader_id")
  private Integer uploaderId;

  @TableField("file_name")
  private String fileName;

  @TableField("original_name")
  private String originalName;

  @TableField("content_type")
  private String contentType;

  @TableField("file_size")
  private Long fileSize;

  private String url;
  private String status;

  @TableField("deleted_by")
  private Integer deletedBy;

  @TableField("deleted_at")
  private Timestamp deletedAt;

  @TableField("created_at")
  private Timestamp createdAt;
}
