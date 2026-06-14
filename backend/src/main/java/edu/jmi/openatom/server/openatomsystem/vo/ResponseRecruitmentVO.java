package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社团招新数据VO
 *
 * <p>包含社团基本信息, 招新活动列表及部门列表, 用于招新管理页面的数据展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseRecruitmentVO {
  private Club club;
  private List<RecruitmentCampaign> campaigns;
  private List<ClubDepartment> departments;
}
