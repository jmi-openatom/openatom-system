package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageDataDTO<T> {
  private List<T> list;
  private Long page;
  private Long pageSize;
  private Long total;
}
