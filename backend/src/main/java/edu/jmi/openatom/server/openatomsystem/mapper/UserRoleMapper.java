package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.UserRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

  /** 按 userId 查 */
  default List<UserRole> selectByUserId(Integer userId) {
    return selectList(
        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
  }

  /** 查用户角色关系 */
  default UserRole selectOneByUserAndRole(Integer userId, Integer roleId) {
    return selectOne(
        new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId)
            .eq(UserRole::getRoleId, roleId));
  }

  /** 按 userId 删除 */
  default int deleteByUserId(Integer userId) {
    return delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
  }

  /** 按 roleId 删除 */
  default int deleteByRoleId(Integer roleId) {
    return delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
  }
}
