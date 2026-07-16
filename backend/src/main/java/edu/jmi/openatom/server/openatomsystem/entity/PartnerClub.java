package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.Data;

/** 开源伙伴公开展示数据。 */
@Data
@TableName("partner_club")
public class PartnerClub {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;

  @TableField("logo_url")
  private String logoUrl;

  private String description;

  @TableField("website_url")
  private String websiteUrl;

  private String organization;
  private String category;
  private String tags;

  @TableField("president_name")
  private String presidentName;

  @TableField("president_avatar_url")
  private String presidentAvatarUrl;

  @TableField("sort_order")
  private Integer sortOrder;

  private Boolean featured;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
