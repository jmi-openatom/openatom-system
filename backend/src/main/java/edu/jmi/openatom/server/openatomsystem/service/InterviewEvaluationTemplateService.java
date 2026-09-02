package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveInterviewEvaluationTemplateDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewEvaluationTemplateVO;
import java.util.List;

public interface InterviewEvaluationTemplateService {
  Result<List<ResponseInterviewEvaluationTemplateVO>> list(Integer campaignId);
  Result<ResponseInterviewEvaluationTemplateVO> active(Integer campaignId);
  Result<ResponseInterviewEvaluationTemplateVO> save(RequestSaveInterviewEvaluationTemplateDTO request);
}
