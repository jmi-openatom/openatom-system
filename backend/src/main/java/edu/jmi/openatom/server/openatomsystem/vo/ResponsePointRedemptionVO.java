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
public class ResponsePointRedemptionVO {
  private Integer id;
  private Integer userId;
  private String userName;
  private String realName;
  private String studentId;
  private Integer itemId;
  private String itemName;
  private Long points;
  private String status;
  private String receiverName;
  private String receiverContact;
  private String remark;
  private String adminNote;
  private Integer reviewedBy;
  private String reviewerName;
  private Timestamp reviewedAt;
  private Timestamp createdAt;
}
