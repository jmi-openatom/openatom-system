package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_profile")
public class MemberProfile {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Integer userId;

  private String slug;

  @TableField("display_name")
  private String displayName;

  private String headline;
  private String bio;

  @TableField("avatar_url")
  private String avatarUrl;

  @TableField("banner_url")
  private String bannerUrl;

  @TableField("card_background_url")
  private String cardBackgroundUrl;

  @TableField("card_focus_x")
  private BigDecimal cardFocusX;

  @TableField("card_focus_y")
  private BigDecimal cardFocusY;

  @TableField("theme_key")
  private String themeKey;

  @TableField("color_mode")
  private String colorMode;

  private String visibility;
  private String status;

  @TableField("comments_enabled")
  private Boolean commentsEnabled;

  @TableField("show_department")
  private Boolean showDepartment;

  @TableField("show_position")
  private Boolean showPosition;

  private String skills;
  private Integer version;

  @TableField("published_at")
  private Timestamp publishedAt;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
