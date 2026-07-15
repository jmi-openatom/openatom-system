package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMemberFilterVO {
  private List<DepartmentOption> departments;
  private List<String> skills;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DepartmentOption {
    private Integer id;
    private String name;
  }
}
