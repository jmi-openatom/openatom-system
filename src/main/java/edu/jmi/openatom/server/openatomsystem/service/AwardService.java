package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import java.util.List;

public interface AwardService {
  ApiResponse<List<ClubAward>> list();

  ApiResponse<String> create(RequestCreateAwardDTO request);

  ApiResponse<String> update(Integer awardId, RequestUpdateAwardDTO request);

  ApiResponse<String> delete(Integer awardId);
}
