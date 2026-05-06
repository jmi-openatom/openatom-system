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
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_membership")
public class ClubMembership {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_id")
  private Integer userId;

  @TableField("club_id")
  private Integer clubId;

  @TableField("department_id")
  private Integer departmentId;

  @TableField("position_id")
  private Integer positionId;

  private String status;

  private Boolean featured;

  @TableField("sort_order")
  private Integer sortOrder;

  private String description;

  @TableField("joined_at")
  private Timestamp joinedAt;

  @TableField("left_at")
  private Timestamp leftAt;
}
