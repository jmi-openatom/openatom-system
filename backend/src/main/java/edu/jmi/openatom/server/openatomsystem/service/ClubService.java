package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRecruitmentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import java.util.List;

/**
 * 社团管理服务接口
 *
 * <p>定义社团的查询列表, 创建, 查看详情, 更新基本信息, 更新状态以及更新招新状态等业务操作
 */
public interface ClubService {
  Result<List<Club>> getClubs(
      String keyword, String category, String status, String recruitmentStatus);

  Result<String> createClub(RequestCreateClubDTO requestCreateClubDTO);

  Result<Club> getClubById(Integer clubId);

  Result<String> updateClub(Integer clubId, RequestUpdateClubDTO requestUpdateClubDTO);

  Result<String> updateStatus(
      Integer clubId, RequestUpdateClubStatusDTO requestUpdateClubStatusDTO);

  Result<String> updateRecruitmentStatus(
      Integer clubId, RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO);
}
