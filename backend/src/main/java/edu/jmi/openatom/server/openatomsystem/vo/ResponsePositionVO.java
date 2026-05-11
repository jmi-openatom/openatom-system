package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职位信息VO
 *
 * <p>包含职位ID, 所属社团与部门, 职位名称, 最大人数限制及关联的角色列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePositionVO {
  private Integer id;
  private Integer clubId;
  private Integer departmentId;
  private String name;
  private Integer maxCount;
  private List<Integer> roleIds;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
