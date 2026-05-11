package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRecruitmentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.service.ClubService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 社团管理实现类
 *
 * <p>负责社团的创建, 更新, 查询以及社团状态和招新状态的变更等业务逻辑
 */
@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {
  private static final List<String> CLUB_STATUSES = List.of("active", "inactive");
  private static final List<String> RECRUITMENT_STATUSES = List.of("open", "closed");

  private final ClubMapper clubMapper;
  private final UserMapper userMapper;

  @Override
  public Result<List<Club>> getClubs(
      String keyword, String category, String status, String recruitmentStatus) {
    return Result.success(clubMapper.selectByConditions(keyword, category, status, recruitmentStatus));
  }

  @Override
  public Result<String> createClub(RequestCreateClubDTO requestCreateClubDTO) {
    if (requestCreateClubDTO == null) {
      return Result.error("请求参数为空");
    }
    if (isBlank(requestCreateClubDTO.getName()) || isBlank(requestCreateClubDTO.getCode())) {
      return Result.error(400, "社团名称和社团编码不能为空");
    }
    if (!userExists(requestCreateClubDTO.getPresidentUserId())) {
      return Result.error(400, "负责人用户不存在");
    }
    if (clubMapper.countByCode(requestCreateClubDTO.getCode()) > 0) {
      return Result.error(400, "社团编码已存在");
    }

    Club club =
        Club.builder()
            .name(requestCreateClubDTO.getName())
            .code(requestCreateClubDTO.getCode())
            .category(requestCreateClubDTO.getCategory())
            .description(requestCreateClubDTO.getDescription())
            .logoUrl(requestCreateClubDTO.getLogoUrl())
            .presidentUserId(requestCreateClubDTO.getPresidentUserId())
            .status("active")
            .recruitmentStatus("closed")
            .build();

    int row = clubMapper.insert(club);
    return row > 0 ? Result.success("社团创建成功") : Result.error("社团创建失败");
  }

  @Override
  public Result<Club> getClubById(Integer clubId) {
    if (clubId == null) {
      return Result.error(400, "clubId不能为空");
    }
    Club club = clubMapper.selectById(clubId);
    if (club == null) {
      return Result.error(404, "社团不存在");
    }
    return Result.success(club);
  }

  @Override
  public Result<String> updateClub(Integer clubId, RequestUpdateClubDTO requestUpdateClubDTO) {
    if (clubId == null) {
      return Result.error(400, "clubId不能为空");
    }
    if (requestUpdateClubDTO == null) {
      return Result.error("请求参数为空");
    }
    Club club = clubMapper.selectById(clubId);
    if (club == null) {
      return Result.error(404, "社团不存在");
    }
    if (requestUpdateClubDTO.getName() != null) {
      if (isBlank(requestUpdateClubDTO.getName())) {
        return Result.error(400, "社团名称不能为空");
      }
      club.setName(requestUpdateClubDTO.getName());
    }
    if (!userExists(requestUpdateClubDTO.getPresidentUserId())) {
      return Result.error(400, "负责人用户不存在");
    }
    if (requestUpdateClubDTO.getCategory() != null) {
      club.setCategory(requestUpdateClubDTO.getCategory());
    }
    if (requestUpdateClubDTO.getDescription() != null) {
      club.setDescription(requestUpdateClubDTO.getDescription());
    }
    if (requestUpdateClubDTO.getLogoUrl() != null) {
      club.setLogoUrl(requestUpdateClubDTO.getLogoUrl());
    }
    if (requestUpdateClubDTO.getPresidentUserId() != null) {
      club.setPresidentUserId(requestUpdateClubDTO.getPresidentUserId());
    }

    int row = clubMapper.updateById(club);
    return row > 0 ? Result.success("社团更新成功") : Result.error("社团更新失败");
  }

  @Override
  public Result<String> updateStatus(
      Integer clubId, RequestUpdateClubStatusDTO requestUpdateClubStatusDTO) {
    if (requestUpdateClubStatusDTO == null || isBlank(requestUpdateClubStatusDTO.getStatus())) {
      return Result.error(400, "社团状态不能为空");
    }
    if (!CLUB_STATUSES.contains(requestUpdateClubStatusDTO.getStatus())) {
      return Result.error(400, "社团状态只能是active或inactive");
    }
    return updateClubField(clubId, requestUpdateClubStatusDTO.getStatus(), null);
  }

  @Override
  public Result<String> updateRecruitmentStatus(
      Integer clubId, RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO) {
    if (requestUpdateRecruitmentStatusDTO == null
        || isBlank(requestUpdateRecruitmentStatusDTO.getRecruitmentStatus())) {
      return Result.error(400, "招新状态不能为空");
    }
    if (!RECRUITMENT_STATUSES.contains(requestUpdateRecruitmentStatusDTO.getRecruitmentStatus())) {
      return Result.error(400, "招新状态只能是open或closed");
    }
    return updateClubField(clubId, null, requestUpdateRecruitmentStatusDTO.getRecruitmentStatus());
  }

  private Result<String> updateClubField(
      Integer clubId, String status, String recruitmentStatus) {
    if (clubId == null) {
      return Result.error(400, "clubId不能为空");
    }
    Club club = clubMapper.selectById(clubId);
    if (club == null) {
      return Result.error(404, "社团不存在");
    }
    if (status != null) {
      club.setStatus(status);
    }
    if (recruitmentStatus != null) {
      club.setRecruitmentStatus(recruitmentStatus);
    }
    int row = clubMapper.updateById(club);
    return row > 0 ? Result.success("社团状态更新成功") : Result.error("社团状态更新失败");
  }

  private boolean userExists(Integer userId) {
    return userId == null || userMapper.selectById(userId) != null;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
