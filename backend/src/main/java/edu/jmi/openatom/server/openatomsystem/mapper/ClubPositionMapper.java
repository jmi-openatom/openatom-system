package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubPositionMapper extends BaseMapper<ClubPosition> {

  /** 按社团查岗位（ordered by id asc） */
  default List<ClubPosition> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubPosition>()
            .eq(ClubPosition::getClubId, clubId)
            .orderByAsc(ClubPosition::getId));
  }

  /** 名称唯一性检查 */
  default Long countByClubIdAndName(Integer clubId, String name, Integer excludeId) {
    LambdaQueryWrapper<ClubPosition> wrapper =
        new LambdaQueryWrapper<ClubPosition>()
            .eq(ClubPosition::getClubId, clubId)
            .eq(ClubPosition::getName, name);
    if (excludeId != null) {
      wrapper.ne(ClubPosition::getId, excludeId);
    }
    return selectCount(wrapper);
  }
}
