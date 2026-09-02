package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveInterviewEvaluationTemplateDTO;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewEvaluationTemplate;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewEvaluationTemplateMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RecruitmentCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.service.InterviewEvaluationTemplateService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewEvaluationTemplateVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewEvaluationTemplateServiceImpl implements InterviewEvaluationTemplateService {
  private final InterviewEvaluationTemplateMapper templateMapper;
  private final RecruitmentCampaignMapper campaignMapper;

  @Override
  public Result<List<ResponseInterviewEvaluationTemplateVO>> list(Integer campaignId) {
    return Result.success(templateMapper.selectForCampaign(campaignId).stream()
        .map(ResponseInterviewEvaluationTemplateVO::from).toList());
  }

  @Override
  public Result<ResponseInterviewEvaluationTemplateVO> active(Integer campaignId) {
    InterviewEvaluationTemplate template = templateMapper.selectActive(campaignId);
    if (template == null) template = templateMapper.selectActive(null);
    return template == null ? Result.error(404, "评价模板不存在")
        : Result.success(ResponseInterviewEvaluationTemplateVO.from(template));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewEvaluationTemplateVO> save(RequestSaveInterviewEvaluationTemplateDTO request) {
    if (campaignMapper.selectById(request.getCampaignId()) == null) return Result.error(404, "招新计划不存在");
    Object dimensions = request.getSchema().get("dimensions");
    if (!(dimensions instanceof List<?> list) || list.isEmpty()) return Result.error(422, "评价模板至少需要一个维度");
    List<InterviewEvaluationTemplate> versions = templateMapper.selectForCampaign(request.getCampaignId());
    int version = versions.stream().map(InterviewEvaluationTemplate::getVersion).filter(v -> v != null)
        .max(Integer::compareTo).orElse(0) + 1;
    versions.stream().filter(t -> "active".equals(t.getStatus())).forEach(t -> {
      t.setStatus("archived"); templateMapper.updateById(t);
    });
    InterviewEvaluationTemplate template = InterviewEvaluationTemplate.builder()
        .campaignId(request.getCampaignId()).name(request.getName().trim())
        .schemaJson(Jsons.stringify(request.getSchema())).version(version).status("active")
        .createdBy(StpUtil.getLoginIdAsInt()).build();
    templateMapper.insert(template);
    return Result.success(ResponseInterviewEvaluationTemplateVO.from(template));
  }
}
