package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.RecruitmentCampaign;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseRecruitmentDTO {
  private Club club;
  private List<RecruitmentCampaign> campaigns;
  private List<ClubDepartment> departments;
}
