package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubVicePresident;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubVicePresidentMapper extends BaseMapper<ClubVicePresident> {

  default List<ClubVicePresident> selectByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubVicePresident>()
            .eq(ClubVicePresident::getClubId, clubId));
  }

  default void deleteByUserId(Integer userId) {
    delete(new LambdaQueryWrapper<ClubVicePresident>()
        .eq(ClubVicePresident::getUserId, userId));
  }

  default void deleteByClubIdAndUserId(Integer clubId, Integer userId) {
    delete(new LambdaQueryWrapper<ClubVicePresident>()
        .eq(ClubVicePresident::getClubId, clubId)
        .eq(ClubVicePresident::getUserId, userId));
  }
}
