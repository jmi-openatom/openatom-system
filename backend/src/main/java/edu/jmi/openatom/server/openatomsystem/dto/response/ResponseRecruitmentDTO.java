package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 社团招新数据DTO
 *
 * <p>包含社团基本信息, 招新活动列表及部门列表, 用于招新管理页面的数据展示
 */
@Data
@Builder
public class ResponseRecruitmentDTO {
  private Club club;
  private List<RecruitmentCampaign> campaigns;
  private List<ClubDepartment> departments;
}
