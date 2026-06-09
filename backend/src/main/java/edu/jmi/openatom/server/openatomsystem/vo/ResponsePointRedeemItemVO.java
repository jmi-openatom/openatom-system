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
public class ResponsePointRedeemItemVO {
  private Integer id;
  private String name;
  private String description;
  private Long pointCost;
  private Integer stock;
  private Integer exchangedCount;
  private Integer availableStock;
  private String status;
  private Integer sortOrder;
  private String imageUrl;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
