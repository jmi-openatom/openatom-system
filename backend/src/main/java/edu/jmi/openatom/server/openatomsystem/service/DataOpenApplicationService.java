package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DataOpenApplication;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseDataOpenApplicationVO;

public interface DataOpenApplicationService {
  Result<ResponseDataOpenApplicationVO> submit(RequestDataOpenApplicationDTO request);

  Result<PageDataVO<ResponseDataOpenApplicationVO>> adminList(
      String keyword, String status, Long page, Long pageSize);

  Result<ResponseDataOpenApplicationVO> review(
      Integer applicationId, RequestReviewDataOpenApplicationDTO request, Integer reviewerId);

  Result<DataOpenApplication> validateApiKey(String apiKey);

  void recordUsage(DataOpenApplication application);
}
