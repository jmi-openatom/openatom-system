package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

  /** 检查 code 是否存在 */
  default Long countByCode(String code) {
    return selectCount(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
  }
}
