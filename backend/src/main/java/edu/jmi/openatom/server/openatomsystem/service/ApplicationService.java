package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;

/**
 * 入社申请服务接口
 *
 * <p>定义入社申请的分页列表, 创建, 查看详情, 更新, 提交和撤回等业务操作
 */
public interface ApplicationService {
  ApiResponse<PageDataDTO<ResponseApplicationDTO>> list(
      Integer campaignId,
      Integer clubId,
      String status,
      Integer departmentId,
      String keyword,
      Long page,
      Long pageSize);

  ApiResponse<Integer> create(RequestCreateApplicationDTO request);

  ApiResponse<MembershipApplication> detail(Integer applicationId);

  ApiResponse<String> update(Integer applicationId, RequestUpdateApplicationDTO request);

  ApiResponse<String> submit(Integer applicationId);

  ApiResponse<String> withdraw(Integer applicationId);
}
