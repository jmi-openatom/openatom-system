package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;

public interface RecruitmentCampaignService {
  ApiResponse<List<RecruitmentCampaign>> listByClub(Integer clubId);

  ApiResponse<String> create(Integer clubId, RequestCreateRecruitmentCampaignDTO request);

  ApiResponse<RecruitmentCampaign> detail(Integer campaignId);

  ApiResponse<String> update(Integer campaignId, RequestUpdateRecruitmentCampaignDTO request);

  ApiResponse<String> publish(Integer campaignId);

  ApiResponse<String> close(Integer campaignId);
}
