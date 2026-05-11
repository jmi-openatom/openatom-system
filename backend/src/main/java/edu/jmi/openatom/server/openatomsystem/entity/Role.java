package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色实体
 *
 * <p>对应数据库表 sys_role, 定义系统角色, 包括角色名称, 编码和数据权限范围
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_role")
public class Role {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;
  private String code;

  @TableField("data_scope")
  private String dataScope;

  private String description;
}
