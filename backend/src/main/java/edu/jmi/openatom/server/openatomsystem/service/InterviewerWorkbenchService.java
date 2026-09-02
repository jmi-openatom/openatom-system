package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewerWorkbenchItemVO;
import java.util.List;

public interface InterviewerWorkbenchService {
  Result<List<ResponseInterviewerWorkbenchItemVO>> list();
}
