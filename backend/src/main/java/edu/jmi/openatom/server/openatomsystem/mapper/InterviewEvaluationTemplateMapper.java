package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewEvaluationTemplate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewEvaluationTemplateMapper extends BaseMapper<InterviewEvaluationTemplate> {
  default List<InterviewEvaluationTemplate> selectForCampaign(Integer campaignId) {
    LambdaQueryWrapper<InterviewEvaluationTemplate> query = new LambdaQueryWrapper<>();
    if (campaignId == null) query.isNull(InterviewEvaluationTemplate::getCampaignId);
    else query.eq(InterviewEvaluationTemplate::getCampaignId, campaignId);
    return selectList(query.orderByDesc(InterviewEvaluationTemplate::getVersion));
  }

  default InterviewEvaluationTemplate selectActive(Integer campaignId) {
    LambdaQueryWrapper<InterviewEvaluationTemplate> query = new LambdaQueryWrapper<InterviewEvaluationTemplate>()
        .eq(InterviewEvaluationTemplate::getStatus, "active");
    if (campaignId == null) query.isNull(InterviewEvaluationTemplate::getCampaignId);
    else query.eq(InterviewEvaluationTemplate::getCampaignId, campaignId);
    return selectOne(query.orderByDesc(InterviewEvaluationTemplate::getVersion).last("LIMIT 1"));
  }
}
