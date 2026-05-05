package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import java.util.List;

public interface ExitApplicationService {
  ApiResponse<List<ExitApplication>> list();

  ApiResponse<String> create(RequestCreateExitApplicationDTO request);

  ApiResponse<ExitApplication> detail(Integer exitApplicationId);

  ApiResponse<String> approve(Integer exitApplicationId);

  ApiResponse<String> reject(Integer exitApplicationId, String comment);
}
