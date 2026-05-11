package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点表单列表DTO
 *
 * <p>包含社团信息及其对应的表单列表, 用于站点表单的管理与展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSiteFormsDTO {
  private Club club;
  private List<SiteForm> forms;
}
