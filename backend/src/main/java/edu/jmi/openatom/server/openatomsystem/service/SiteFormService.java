package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateSiteFormDTO;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import java.util.List;

/**
 * 站点表单服务接口
 *
 * <p>定义站点表单的按社团查询列表, 创建, 查看详情, 更新, 发布和关闭等业务操作
 */
public interface SiteFormService {
  Result<List<SiteForm>> listByClub(Integer clubId);

  Result<String> create(Integer clubId, RequestCreateSiteFormDTO request);

  Result<SiteForm> detail(Integer formId);

  Result<String> update(Integer formId, RequestUpdateSiteFormDTO request);

  Result<String> publish(Integer formId);

  Result<String> close(Integer formId);
}
