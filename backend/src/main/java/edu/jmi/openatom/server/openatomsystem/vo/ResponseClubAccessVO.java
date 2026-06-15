package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 当前用户的社团访问范围。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseClubAccessVO {
  private Integer clubId;
  private String clubName;
  private String clubCode;
  private String category;
  private String status;
  private String membershipStatus;
  private Integer departmentId;
  private Integer positionId;
  private Boolean manageable;
  private Boolean superAdmin;
  private List<String> roleCodes;
}
