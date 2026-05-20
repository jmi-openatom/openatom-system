package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBotCreateLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewLeaveApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLeaveApplicationVO;
import java.util.List;

public interface LeaveApplicationService {
  Result<List<ResponseLeaveApplicationVO>> list(String status);

  Result<List<ResponseLeaveApplicationVO>> mine();

  Result<ResponseLeaveApplicationVO> detail(Integer leaveApplicationId);

  Result<ResponseLeaveApplicationVO> botDetailByQq(Integer leaveApplicationId, String qqOpenid);

  Result<Integer> create(RequestCreateLeaveApplicationDTO request);

  Result<Integer> createByQq(RequestBotCreateLeaveApplicationDTO request);

  Result<String> review(Integer leaveApplicationId, RequestReviewLeaveApplicationDTO request);

  Result<String> delete(Integer leaveApplicationId);
}
