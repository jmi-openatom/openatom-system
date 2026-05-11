package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点表单详情DTO
 *
 * <p>包含社团信息及其对应的单个表单详情, 用于站点表单的详细查看
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSiteFormDetailDTO {
  private Club club;
  private SiteForm form;
}
