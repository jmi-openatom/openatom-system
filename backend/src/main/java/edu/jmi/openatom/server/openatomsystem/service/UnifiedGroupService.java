package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUnifiedGroupDTO;
import java.util.List;
import java.util.Map;

public interface UnifiedGroupService {
  Result<List<Map<String, Object>>> list(Integer clubId, String type, String keyword);

  Result<String> create(RequestCreateUnifiedGroupDTO request);

  Result<String> update(Long groupId, RequestUpdateUnifiedGroupDTO request);

  Result<Map<String, Object>> userOptions(
      Integer clubId, String keyword, String status, Integer departmentId, Integer page, Integer pageSize);

  Result<List<Integer>> userOptionIds(
      Integer clubId, String keyword, String status, Integer departmentId);

  Result<Map<String, Object>> detail(Long groupId);

  Result<List<Map<String, Object>>> members(Long groupId);

  Result<Map<String, Object>> dependencies(Long groupId);

  Result<String> synchronize();
}
