package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 社团规章制度响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseClubRegulationVO {
  private Integer id;
  private Integer clubId;
  private String clubName;
  private String title;
  private String summary;
  private String contentMarkdown;
  private String status;
  private Integer sortOrder;
  private Timestamp publishedAt;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
