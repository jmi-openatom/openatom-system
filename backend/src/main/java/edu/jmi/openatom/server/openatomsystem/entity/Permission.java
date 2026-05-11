package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限实体
 *
 * <p>对应数据库表 sys_permission, 定义系统中的权限项, 包括权限名称, 编码, 类型和接口路径
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_permission")
public class Permission {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String name;
  private String code;
  private String type;
  private String path;
  private String method;
}
