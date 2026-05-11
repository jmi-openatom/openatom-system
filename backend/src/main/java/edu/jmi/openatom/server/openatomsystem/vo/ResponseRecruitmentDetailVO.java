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
 * 招新详情VO
 *
 * <p>包含社团信息, 当前招新活动以及可选部门列表, 用于招新申请的详细信息展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseRecruitmentDetailVO {
  private Club club;
  private RecruitmentCampaign campaign;
  private List<ClubDepartment> departments;
}
