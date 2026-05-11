package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RecruitmentCampaignMapper;
import edu.jmi.openatom.server.openatomsystem.service.RecruitmentCampaignService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 招新计划管理实现类
 *
 * <p>负责招新计划的创建, 更新, 查询, 发布和关闭等业务逻辑, 包含招新表单Schema管理
 */
@Service
@RequiredArgsConstructor
public class RecruitmentCampaignServiceImpl implements RecruitmentCampaignService {
  private static final List<String> STATUSES = List.of("draft", "published", "open", "closed", "archived");

  private final RecruitmentCampaignMapper recruitmentCampaignMapper;
  private final ClubMapper clubMapper;

  @Override
  public ApiResponse<List<RecruitmentCampaign>> listByClub(Integer clubId) {
    if (clubId == null) return ApiResponse.error(400, "clubId不能为空");
    if (clubMapper.selectById(clubId) == null) return ApiResponse.error(404, "社团不存在");
    return ApiResponse.success(recruitmentCampaignMapper.selectByClubIdOrdered(clubId));
  }

  @Override
  public ApiResponse<String> create(Integer clubId, RequestCreateRecruitmentCampaignDTO request) {
    if (clubId == null) return ApiResponse.error(400, "clubId不能为空");
    Club club = clubMapper.selectById(clubId);
    if (club == null) return ApiResponse.error(404, "社团不存在");
    String status = request.getStatus() == null ? "draft" : request.getStatus();
    if (!STATUSES.contains(status)) return ApiResponse.error(400, "招新计划状态不合法");
    RecruitmentCampaign campaign = RecruitmentCampaign.builder().clubId(clubId).name(request.getName())
        .applyStartAt(Times.parseTimestamp(request.getApplyStartAt())).applyEndAt(Times.parseTimestamp(request.getApplyEndAt()))
        .interviewStartAt(Times.parseTimestamp(request.getInterviewStartAt())).interviewEndAt(Times.parseTimestamp(request.getInterviewEndAt()))
        .resultPublishAt(Times.parseTimestamp(request.getResultPublishAt())).targetGrades(Jsons.stringify(request.getTargetGrades()))
        .maxApplicants(request.getMaxApplicants()).loginRequired(Boolean.TRUE.equals(request.getLoginRequired()))
        .formSchema(Jsons.stringify(request.getFormSchema())).status(status).build();
    int row = recruitmentCampaignMapper.insert(campaign);
    return row > 0 ? ApiResponse.success("招新计划创建成功") : ApiResponse.error("招新计划创建失败");
  }

  @Override
  public ApiResponse<RecruitmentCampaign> detail(Integer campaignId) {
    RecruitmentCampaign campaign = findCampaign(campaignId);
    return campaign == null ? ApiResponse.error(404, "招新计划不存在") : ApiResponse.success(campaign);
  }

  @Override
  public ApiResponse<String> update(Integer campaignId, RequestUpdateRecruitmentCampaignDTO request) {
    RecruitmentCampaign campaign = findCampaign(campaignId);
    if (campaign == null) return ApiResponse.error(404, "招新计划不存在");
    if (request.getName() != null) campaign.setName(request.getName());
    if (request.getApplyStartAt() != null) campaign.setApplyStartAt(Times.parseTimestamp(request.getApplyStartAt()));
    if (request.getApplyEndAt() != null) campaign.setApplyEndAt(Times.parseTimestamp(request.getApplyEndAt()));
    if (request.getInterviewStartAt() != null) campaign.setInterviewStartAt(Times.parseTimestamp(request.getInterviewStartAt()));
    if (request.getInterviewEndAt() != null) campaign.setInterviewEndAt(Times.parseTimestamp(request.getInterviewEndAt()));
    if (request.getResultPublishAt() != null) campaign.setResultPublishAt(Times.parseTimestamp(request.getResultPublishAt()));
    if (request.getTargetGrades() != null) campaign.setTargetGrades(Jsons.stringify(request.getTargetGrades()));
    if (request.getMaxApplicants() != null) campaign.setMaxApplicants(request.getMaxApplicants());
    if (request.getLoginRequired() != null) campaign.setLoginRequired(request.getLoginRequired());
    if (request.getFormSchema() != null) campaign.setFormSchema(Jsons.stringify(request.getFormSchema()));
    if (request.getStatus() != null) {
      if (!STATUSES.contains(request.getStatus())) return ApiResponse.error(400, "招新计划状态不合法");
      campaign.setStatus(request.getStatus());
    }
    int row = recruitmentCampaignMapper.updateById(campaign);
    return row > 0 ? ApiResponse.success("招新计划更新成功") : ApiResponse.error("招新计划更新失败");
  }

  @Override
  public ApiResponse<String> publish(Integer campaignId) { return updateStatus(campaignId, "open", "招新计划发布成功"); }

  @Override
  public ApiResponse<String> close(Integer campaignId) { return updateStatus(campaignId, "closed", "招新计划关闭成功"); }

  private ApiResponse<String> updateStatus(Integer campaignId, String status, String message) {
    RecruitmentCampaign campaign = findCampaign(campaignId);
    if (campaign == null) return ApiResponse.error(404, "招新计划不存在");
    campaign.setStatus(status);
    int row = recruitmentCampaignMapper.updateById(campaign);
    return row > 0 ? ApiResponse.success(message) : ApiResponse.error("招新计划状态更新失败");
  }

  private RecruitmentCampaign findCampaign(Integer campaignId) { return campaignId == null ? null : recruitmentCampaignMapper.selectById(campaignId); }
}
