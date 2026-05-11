package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import java.util.List;

/**
 * 社团奖项服务接口
 *
 * <p>定义社团奖项的列表查询, 创建, 更新和删除等业务操作
 */
public interface AwardService {
  Result<List<ClubAward>> list();

  Result<String> create(RequestCreateAwardDTO request);

  Result<String> update(Integer awardId, RequestUpdateAwardDTO request);

  Result<String> delete(Integer awardId);
}
