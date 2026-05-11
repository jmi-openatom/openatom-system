package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色权限关联
 *
 * <p>对应数据库表 sys_role_permission, 建立角色与权限之间的多对多关联关系
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_role_permission")
public class RolePermission {
  /** 角色ID */
  private Integer roleId;

  /** 权限ID */
  private Integer permissionId;
}
