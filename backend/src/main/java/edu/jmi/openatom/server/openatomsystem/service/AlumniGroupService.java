package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import java.util.List;

/**
 * 往届管理人员分组服务接口
 */
public interface AlumniGroupService {
  Result<List<ClubAlumniGroup>> listByClubId(Integer clubId);

  Result<String> create(Integer clubId, RequestCreateAlumniGroupDTO request);

  Result<String> update(Integer groupId, RequestUpdateAlumniGroupDTO request);

  Result<String> delete(Integer groupId);
}
