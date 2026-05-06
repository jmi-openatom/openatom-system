package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseClubHomeDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteProgressDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormsDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseSiteFormDetailDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseRecruitmentDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

public interface SiteService {
  ApiResponse<ResponseClubHomeDTO> getClubHome(String clubCode);

  ApiResponse<List<ClubActivity>> getActivities();

  ApiResponse<ClubActivity> getActivityDetail(Integer activityId);

  ApiResponse<List<Club>> getPublicClubs();

  ApiResponse<ResponseSiteFormsDTO> getPublicForms(Integer clubId);

  ApiResponse<ResponseRecruitmentDTO> getRecruitment(Integer clubId);

  ApiResponse<ResponseSiteFormDetailDTO> getFormDetail(Integer campaignId);

  ApiResponse<ResponseSiteProgressDTO> getMyProgress();

  ApiResponse<Boolean> getRegisterEnabled();
}
