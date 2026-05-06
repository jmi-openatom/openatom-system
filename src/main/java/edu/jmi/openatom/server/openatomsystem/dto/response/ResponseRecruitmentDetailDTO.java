package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseRecruitmentDetailDTO {
  private Club club;
  private RecruitmentCampaign campaign;
  private List<ClubDepartment> departments;
}
