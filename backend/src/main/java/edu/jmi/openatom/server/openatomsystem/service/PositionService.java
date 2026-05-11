package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePositionVO;
import java.util.List;

/**
 * 职位管理服务接口
 *
 * <p>定义社团职位的按社团查询列表, 创建, 查看详情, 更新和删除等业务操作
 */
public interface PositionService {
  Result<List<ResponsePositionVO>> getPositionsByClubId(Integer clubId);

  Result<String> createPosition(
      Integer clubId, RequestCreatePositionDTO requestCreatePositionDTO);

  Result<ResponsePositionVO> getPositionById(Integer positionId);

  Result<String> updatePosition(
      Integer positionId, RequestUpdatePositionDTO requestUpdatePositionDTO);

  Result<String> deletePosition(Integer positionId);
}
