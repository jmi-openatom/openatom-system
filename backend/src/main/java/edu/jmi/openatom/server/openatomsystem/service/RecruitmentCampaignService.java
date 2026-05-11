package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRecruitmentCampaignDTO;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;

/**
 * 招新活动服务接口
 *
 * <p>定义招新活动的按社团查询列表, 创建, 查看详情, 更新, 发布和关闭等业务操作
 */
public interface RecruitmentCampaignService {
  Result<List<RecruitmentCampaign>> listByClub(Integer clubId);

  Result<String> create(Integer clubId, RequestCreateRecruitmentCampaignDTO request);

  Result<RecruitmentCampaign> detail(Integer campaignId);

  Result<String> update(Integer campaignId, RequestUpdateRecruitmentCampaignDTO request);

  Result<String> publish(Integer campaignId);

  Result<String> close(Integer campaignId);
}
