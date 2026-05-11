package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.UserRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联数据访问层
 *
 * <p>提供对用户角色关联关系(UserRole)的数据库操作, 包括按用户ID查询角色, 按用户和角色查询关联关系以及按用户ID或角色ID删除关联关系等功能
 */
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
