package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

public interface ActivityService {
  ApiResponse<List<ClubActivity>> list(String status);

  ApiResponse<ClubActivity> detail(Integer activityId);

  ApiResponse<String> create(RequestCreateActivityDTO request);

  ApiResponse<String> update(Integer activityId, RequestUpdateActivityDTO request);

  ApiResponse<String> delete(Integer activityId);

  ApiResponse<String> register(Integer activityId, RequestActivityRegistrationDTO request);

  ApiResponse<List<ActivityRegistration>> registrations(Integer activityId);
}
