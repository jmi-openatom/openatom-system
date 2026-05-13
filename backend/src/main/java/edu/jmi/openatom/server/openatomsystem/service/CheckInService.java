package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInScanDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateCheckInSessionDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCheckInSessionVO;
import java.util.List;

public interface CheckInService {
  Result<List<ResponseCheckInSessionVO>> list(String status);

  Result<ResponseCheckInSessionVO> detail(Integer sessionId);

  Result<Integer> create(RequestCreateCheckInSessionDTO request);

  Result<String> close(Integer sessionId);

  Result<String> delete(Integer sessionId);

  Result<List<ResponseCheckInRecordVO>> records(Integer sessionId);

  Result<ResponseCheckInRecordVO> scan(RequestCheckInScanDTO request);
}
