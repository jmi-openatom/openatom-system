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
public class ResponsePointTransactionVO {
  private Integer id;
  private Integer userId;
  private String userName;
  private String realName;
  private String studentId;
  private Integer delta;
  private Integer balanceAfter;
  private String type;
  private String sourceType;
  private Integer sourceId;
  private String sourceKey;
  private String description;
  private Integer operatorId;
  private String operatorName;
  private Timestamp createdAt;
}
