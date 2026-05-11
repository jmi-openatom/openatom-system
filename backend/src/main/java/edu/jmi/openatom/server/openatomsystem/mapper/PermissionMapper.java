package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限数据访问层
 *
 * <p>提供对权限(Permission)的数据库操作, 包括检查权限编码是否存在等功能
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

  /** 检查 code 是否存在 */
  default Long countByCode(String code) {
    return selectCount(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
  }
}
