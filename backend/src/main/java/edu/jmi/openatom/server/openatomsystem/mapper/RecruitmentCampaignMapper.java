package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecruitmentCampaignMapper extends BaseMapper<RecruitmentCampaign> {

  /** 按社团查招新计划（ordered by applyStartAt desc） */
  default List<RecruitmentCampaign> selectByClubIdOrderedByApplyStart(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<RecruitmentCampaign>()
            .eq(RecruitmentCampaign::getClubId, clubId)
            .orderByDesc(RecruitmentCampaign::getApplyStartAt));
  }

  /** 按社团查招新计划（ordered by id desc） */
  default List<RecruitmentCampaign> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<RecruitmentCampaign>()
            .eq(RecruitmentCampaign::getClubId, clubId)
            .orderByDesc(RecruitmentCampaign::getId));
  }

  /** 查 open/published 状态的招新计划 */
  default List<RecruitmentCampaign> selectOpenByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<RecruitmentCampaign>()
            .eq(RecruitmentCampaign::getClubId, clubId)
            .in(RecruitmentCampaign::getStatus, List.of("open", "published"))
            .orderByDesc(RecruitmentCampaign::getApplyStartAt)
            .orderByDesc(RecruitmentCampaign::getId));
  }
}
