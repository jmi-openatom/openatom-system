package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import java.util.List;

public interface SiteFormService {
  ApiResponse<List<SiteForm>> listByClub(Integer clubId);

  ApiResponse<String> create(Integer clubId, RequestCreateSiteFormDTO request);

  ApiResponse<SiteForm> detail(Integer formId);

  ApiResponse<String> update(Integer formId, RequestUpdateSiteFormDTO request);

  ApiResponse<String> publish(Integer formId);

  ApiResponse<String> close(Integer formId);
}
