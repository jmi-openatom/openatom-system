package edu.jmi.openatom.server.openatomsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseOfficeDocumentUserOptionDTO {
  private Integer id;
  private String realName;
  private String studentId;
  private String college;
  private String major;
  private String grade;
  private String className;
  private String phone;
}
