package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社团获奖数据访问层
 *
 * <p>提供对社团获奖记录(ClubAward)的数据库操作, 包括按社团ID查询获奖列表, 按ID和社团ID查询单条获奖记录等功能
 */
@Mapper
public interface ClubAwardMapper extends BaseMapper<ClubAward> {

  /** 按社团查获奖（含排序） */
  default List<ClubAward> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubAward>()
            .eq(ClubAward::getClubId, clubId)
            .orderByDesc(ClubAward::getAwardYear)
            .orderByAsc(ClubAward::getSortOrder)
            .orderByDesc(ClubAward::getId));
  }

  /** 按ID和社团ID查单条 */
  default ClubAward selectOneByIdAndClubId(Integer awardId, Integer clubId) {
    return selectOne(
        new LambdaQueryWrapper<ClubAward>()
            .eq(ClubAward::getId, awardId)
            .eq(ClubAward::getClubId, clubId)
            .last("LIMIT 1"));
  }
}
