package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePointAccountVO {
  private Integer userId;
  private String userName;
  private String realName;
  private String studentId;
  private String className;
  private Integer balance;
  private Integer totalEarned;
  private Integer totalSpent;
  private Integer rank;
  private Timestamp updatedAt;
}
