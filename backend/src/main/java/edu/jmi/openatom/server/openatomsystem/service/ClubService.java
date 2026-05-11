package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import java.util.List;

/**
 * 社团管理服务接口
 *
 * <p>定义社团的查询列表, 创建, 查看详情, 更新基本信息, 更新状态以及更新招新状态等业务操作
 */
public interface ClubService {
  ApiResponse<List<Club>> getClubs(
      String keyword, String category, String status, String recruitmentStatus);

  ApiResponse<String> createClub(RequestCreateClubDTO requestCreateClubDTO);

  ApiResponse<Club> getClubById(Integer clubId);

  ApiResponse<String> updateClub(Integer clubId, RequestUpdateClubDTO requestUpdateClubDTO);

  ApiResponse<String> updateStatus(
      Integer clubId, RequestUpdateClubStatusDTO requestUpdateClubStatusDTO);

  ApiResponse<String> updateRecruitmentStatus(
      Integer clubId, RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO);
}
