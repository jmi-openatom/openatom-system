package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseApplicationVO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;

/**
 * 入社申请服务接口
 *
 * <p>定义入社申请的分页列表, 创建, 查看详情, 更新, 提交和撤回等业务操作
 */
public interface ApplicationService {
  Result<PageDataVO<ResponseApplicationVO>> list(
      Integer campaignId,
      Integer clubId,
      String status,
      Integer departmentId,
      String keyword,
      Long page,
      Long pageSize);

  Result<Integer> create(RequestCreateApplicationDTO request);

  Result<MembershipApplication> detail(Integer applicationId);

  Result<String> update(Integer applicationId, RequestUpdateApplicationDTO request);

  Result<String> submit(Integer applicationId);

  Result<String> withdraw(Integer applicationId);
}
