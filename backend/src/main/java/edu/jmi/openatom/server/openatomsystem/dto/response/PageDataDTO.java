package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页数据DTO
 *
 * <p>封装分页查询结果, 包含数据列表, 当前页码, 每页大小及总记录数
 */
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
