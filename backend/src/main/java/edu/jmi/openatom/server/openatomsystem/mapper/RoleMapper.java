package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

  /** 按 code 查角色 */
  default Role selectByCode(String code) {
    return selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
  }

  /** 按 code 查角色（排除指定ID） */
  default Role selectByCodeExcludeId(String code, Integer excludeId) {
    return selectOne(
        new LambdaQueryWrapper<Role>()
            .eq(Role::getCode, code)
            .ne(Role::getId, excludeId));
  }

  /** 检查 code 是否存在 */
  default Long countByCode(String code) {
    return selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
  }
}
