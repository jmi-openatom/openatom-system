package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社团职位角色关联
 *
 * <p>对应数据库表 club_position_role, 建立社团职位与系统角色之间的关联关系
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("club_position_role")
public class ClubPositionRole {
  @TableField("position_id")
  private Integer positionId;

  @TableField("role_id")
  private Integer roleId;
}
