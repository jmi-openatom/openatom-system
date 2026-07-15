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
@TableName("member_profile_social_link")
public class MemberProfileSocialLink {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("profile_id")
  private Long profileId;

  private String platform;
  private String label;
  private String url;

  @TableField("sort_order")
  private Integer sortOrder;

  private Boolean enabled;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
