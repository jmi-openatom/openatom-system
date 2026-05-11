package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import java.util.List;

/**
 * 站点表单服务接口
 *
 * <p>定义站点表单的按社团查询列表, 创建, 查看详情, 更新, 发布和关闭等业务操作
 */
public interface SiteFormService {
  ApiResponse<List<SiteForm>> listByClub(Integer clubId);

  ApiResponse<String> create(Integer clubId, RequestCreateSiteFormDTO request);

  ApiResponse<SiteForm> detail(Integer formId);

  ApiResponse<String> update(Integer formId, RequestUpdateSiteFormDTO request);

  ApiResponse<String> publish(Integer formId);

  ApiResponse<String> close(Integer formId);
}
