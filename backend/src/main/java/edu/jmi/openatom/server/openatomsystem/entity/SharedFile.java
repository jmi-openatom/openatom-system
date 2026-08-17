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

/** 共享文件架：目录 + 文件（任意类型），可设访问密码。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("shared_file")
public class SharedFile {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("parent_id")
  private Long parentId;

  @TableField("name")
  private String name;

  @TableField("is_dir")
  private Boolean dir;

  @TableField("extension")
  private String extension;

  @TableField("size_bytes")
  private Long sizeBytes;

  @TableField("storage_name")
  private String storageName;

  @TableField("password_hash")
  private String passwordHash;

  @TableField("owner_user_id")
  private Integer ownerUserId;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
