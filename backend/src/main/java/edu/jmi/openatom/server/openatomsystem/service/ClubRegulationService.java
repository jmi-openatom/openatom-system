package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveClubRegulationDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubRegulationVO;
import java.util.List;

/** 社团规章制度服务 */
public interface ClubRegulationService {
  Result<List<ResponseClubRegulationVO>> list(Integer clubId, String status, String keyword);

  Result<ResponseClubRegulationVO> detail(Integer regulationId);

  Result<ResponseClubRegulationVO> create(
      Integer clubId, RequestSaveClubRegulationDTO request);

  Result<String> update(Integer regulationId, RequestSaveClubRegulationDTO request);

  Result<String> delete(Integer regulationId);

  Result<List<ResponseClubRegulationVO>> publicList(Integer clubId, String keyword);

  Result<ResponseClubRegulationVO> publicDetail(Integer regulationId);
}
