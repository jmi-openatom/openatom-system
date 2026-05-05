package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePositionDTO {
  private Integer id;
  private Integer clubId;
  private Integer departmentId;
  private String name;
  private Integer maxCount;
  private List<Integer> roleIds;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
