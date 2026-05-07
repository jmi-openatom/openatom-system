package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.RolePermission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

  /** 按角色ID列表查 */
  default List<RolePermission> selectByRoleIds(List<Integer> roleIds) {
    return selectList(
        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
  }

  /** 按 roleId 删除 */
  default int deleteByRoleId(Integer roleId) {
    return delete(
        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
  }
}
