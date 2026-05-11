package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社团部门数据访问层
 *
 * <p>提供对社团部门(ClubDepartment)的数据库操作, 包括按社团ID查询部门列表, 检查部门名称唯一性以及清空部门负责人等功能
 */
@Mapper
public interface ClubDepartmentMapper extends BaseMapper<ClubDepartment> {

  /** 按社团查部门（orderByAsc id） */
  default List<ClubDepartment> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubDepartment>()
            .eq(ClubDepartment::getClubId, clubId)
            .orderByAsc(ClubDepartment::getId));
  }

  /** 检查部门名称唯一性（排除自身） */
  default Long countByClubIdAndName(Integer clubId, String name, Integer excludeId) {
    LambdaQueryWrapper<ClubDepartment> wrapper =
        new LambdaQueryWrapper<ClubDepartment>()
            .eq(ClubDepartment::getClubId, clubId)
            .eq(ClubDepartment::getName, name);
    if (excludeId != null) {
      wrapper.ne(ClubDepartment::getId, excludeId);
    }
    return selectCount(wrapper);
  }

  /** 清空负责人 (用户被删除时) */
  default void nullifyManagerUserId(Integer userId) {
    update(
        null,
        new LambdaUpdateWrapper<ClubDepartment>()
            .eq(ClubDepartment::getManagerUserId, userId)
            .set(ClubDepartment::getManagerUserId, null));
  }
}
