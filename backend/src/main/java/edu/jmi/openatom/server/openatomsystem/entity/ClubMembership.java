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
 * 社团成员关系
 *
 * <p>对应数据库表 club_membership, 记录用户与社团的成员关系, 包括所属部门, 职位和状态
 */
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

  @TableField("joined_at")
  private Timestamp joinedAt;

  @TableField("left_at")
  private Timestamp leftAt;

  @TableField("alumni_group")
  private String alumniGroup;

  @TableField("alumni_group_id")
  private Integer alumniGroupId;
}
