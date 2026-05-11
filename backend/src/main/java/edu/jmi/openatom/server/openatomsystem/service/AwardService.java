package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import java.util.List;

/**
 * 社团奖项服务接口
 *
 * <p>定义社团奖项的列表查询, 创建, 更新和删除等业务操作
 */
public interface AwardService {
  ApiResponse<List<ClubAward>> list();

  ApiResponse<String> create(RequestCreateAwardDTO request);

  ApiResponse<String> update(Integer awardId, RequestUpdateAwardDTO request);

  ApiResponse<String> delete(Integer awardId);
}
