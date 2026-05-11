package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联
 *
 * <p>对应数据库表 sys_user_role, 建立用户与角色之间的多对多关联关系
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user_role")
public class UserRole {
  /** 用户ID */
  private Integer userId;

  /** 角色ID */
  private Integer roleId;
}
