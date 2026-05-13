package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveSchoolCalendarDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseSchoolCalendarVO;

public interface SchoolCalendarService {
  Result<ResponseSchoolCalendarVO> detail();

  Result<ResponseSchoolCalendarVO> save(RequestSaveSchoolCalendarDTO request);

  Result<ResponseSchoolCalendarVO> saveAdjustment(RequestSaveSchoolCalendarAdjustmentDTO request);

  Result<String> deleteAdjustment(Integer adjustmentId);
}
