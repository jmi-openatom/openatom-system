package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponsePositionDTO;
import java.util.List;

/**
 * 职位管理服务接口
 *
 * <p>定义社团职位的按社团查询列表, 创建, 查看详情, 更新和删除等业务操作
 */
public interface PositionService {
  ApiResponse<List<ResponsePositionDTO>> getPositionsByClubId(Integer clubId);

  ApiResponse<String> createPosition(
      Integer clubId, RequestCreatePositionDTO requestCreatePositionDTO);

  ApiResponse<ResponsePositionDTO> getPositionById(Integer positionId);

  ApiResponse<String> updatePosition(
      Integer positionId, RequestUpdatePositionDTO requestUpdatePositionDTO);

  ApiResponse<String> deletePosition(Integer positionId);
}
