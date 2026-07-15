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
@TableName("member_profile_module")
public class MemberProfileModule {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("profile_id")
  private Long profileId;

  @TableField("module_key")
  private String moduleKey;

  private String title;

  @TableField("sort_order")
  private Integer sortOrder;

  @TableField("column_span")
  private Integer columnSpan;

  private Boolean enabled;
  private String visibility;

  @TableField("config_json")
  private String configJson;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
