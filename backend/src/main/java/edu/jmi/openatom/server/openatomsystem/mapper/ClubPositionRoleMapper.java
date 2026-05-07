package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPositionRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubPositionRoleMapper extends BaseMapper<ClubPositionRole> {

  /** 按岗位ID查 */
  default List<ClubPositionRole> selectByPositionId(Integer positionId) {
    return selectList(
        new LambdaQueryWrapper<ClubPositionRole>()
            .eq(ClubPositionRole::getPositionId, positionId));
  }

  /** 按岗位ID删除 */
  default int deleteByPositionId(Integer positionId) {
    return delete(
        new LambdaQueryWrapper<ClubPositionRole>()
            .eq(ClubPositionRole::getPositionId, positionId));
  }
}
