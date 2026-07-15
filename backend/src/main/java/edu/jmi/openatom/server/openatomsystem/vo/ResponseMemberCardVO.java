package edu.jmi.openatom.server.openatomsystem.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMemberCardVO {
  private String slug;
  private String displayName;
  private String headline;
  private String avatarUrl;
  private String cardBackgroundUrl;
  private BigDecimal cardFocusX;
  private BigDecimal cardFocusY;
  private String departmentName;
  private String positionName;
  private List<String> skills;
  private Long articleCount;
  private Long likeCount;
  private Boolean liked;
  private Boolean customized;
}
