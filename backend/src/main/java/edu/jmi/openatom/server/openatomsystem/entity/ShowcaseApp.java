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
 * 应用展示条目
 *
 * <p>保存社团对外展示的应用项目、开源仓库、版本、协议和下载入口等信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("showcase_app")
public class ShowcaseApp {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;
  private String summary;

  @TableField("cover_url")
  private String coverUrl;

  @TableField("open_source")
  private Boolean openSource;

  @TableField("github_url")
  private String githubUrl;

  @TableField("gitee_url")
  private String giteeUrl;

  private String developer;
  private String owner;

  @TableField("license_name")
  private String licenseName;

  private String version;

  @TableField("app_status")
  private String appStatus;

  @TableField("download_url")
  private String downloadUrl;

  private String status;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("updated_by")
  private Integer updatedBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
