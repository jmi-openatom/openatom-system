package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;

/**
 * 招新活动服务接口
 *
 * <p>定义招新活动的按社团查询列表, 创建, 查看详情, 更新, 发布和关闭等业务操作
 */
public interface RecruitmentCampaignService {
  ApiResponse<List<RecruitmentCampaign>> listByClub(Integer clubId);

  ApiResponse<String> create(Integer clubId, RequestCreateRecruitmentCampaignDTO request);

  ApiResponse<RecruitmentCampaign> detail(Integer campaignId);

  ApiResponse<String> update(Integer campaignId, RequestUpdateRecruitmentCampaignDTO request);

  ApiResponse<String> publish(Integer campaignId);

  ApiResponse<String> close(Integer campaignId);
}
