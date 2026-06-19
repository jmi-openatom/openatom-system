package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubRegulation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 社团规章制度数据访问层 */
@Mapper
public interface ClubRegulationMapper extends BaseMapper<ClubRegulation> {
  default List<ClubRegulation> selectByConditions(
      Integer clubId, String status, String keyword) {
    String normalizedKeyword = keyword == null ? null : keyword.trim();
    return selectList(
        new LambdaQueryWrapper<ClubRegulation>()
            .eq(clubId != null, ClubRegulation::getClubId, clubId)
            .eq(status != null && !status.isBlank(), ClubRegulation::getStatus, status)
            .and(
                normalizedKeyword != null && !normalizedKeyword.isBlank(),
                wrapper ->
                    wrapper
                        .like(ClubRegulation::getTitle, normalizedKeyword)
                        .or()
                        .like(ClubRegulation::getSummary, normalizedKeyword)
                        .or()
                        .like(ClubRegulation::getContentMarkdown, normalizedKeyword))
            .orderByAsc(ClubRegulation::getSortOrder)
            .orderByDesc(ClubRegulation::getPublishedAt)
            .orderByDesc(ClubRegulation::getId));
  }

  default ClubRegulation selectPublishedById(Integer regulationId) {
    return selectOne(
        new LambdaQueryWrapper<ClubRegulation>()
            .eq(ClubRegulation::getId, regulationId)
            .eq(ClubRegulation::getStatus, "published"));
  }
}
