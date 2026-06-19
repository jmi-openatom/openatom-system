package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveClubRegulationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubRegulation;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubRegulationMapper;
import edu.jmi.openatom.server.openatomsystem.service.ClubRegulationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubRegulationVO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 社团规章制度服务实现 */
@Service
@RequiredArgsConstructor
public class ClubRegulationServiceImpl implements ClubRegulationService {
  private static final Set<String> STATUSES = Set.of("draft", "published");

  private final ClubRegulationMapper clubRegulationMapper;
  private final ClubMapper clubMapper;

  @Override
  public Result<List<ResponseClubRegulationVO>> list(
      Integer clubId, String status, String keyword) {
    String normalizedStatus = normalizeStatus(status, null);
    if (trimToNull(status) != null && normalizedStatus == null) {
      return Result.error(400, "制度状态只能是 draft 或 published");
    }
    List<ClubRegulation> rows =
        clubRegulationMapper.selectByConditions(clubId, normalizedStatus, keyword);
    return Result.success(toResponses(rows, true));
  }

  @Override
  public Result<ResponseClubRegulationVO> detail(Integer regulationId) {
    ClubRegulation regulation = find(regulationId);
    if (regulation == null) return Result.error(404, "规章制度不存在");
    return Result.success(toResponse(regulation, clubMapper.selectById(regulation.getClubId())));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseClubRegulationVO> create(
      Integer clubId, RequestSaveClubRegulationDTO request) {
    Club club = clubId == null ? null : clubMapper.selectById(clubId);
    if (club == null) return Result.error(404, "社团不存在");
    String status = normalizeStatus(request.getStatus(), "draft");
    if (status == null) return Result.error(400, "制度状态只能是 draft 或 published");

    Integer userId = currentUserId();
    ClubRegulation regulation =
        ClubRegulation.builder()
            .clubId(clubId)
            .title(request.getTitle().trim())
            .summary(trimToNull(request.getSummary()))
            .contentMarkdown(request.getContentMarkdown().trim())
            .status(status)
            .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
            .createdBy(userId)
            .updatedBy(userId)
            .publishedAt("published".equals(status) ? Times.now() : null)
            .build();
    int row = clubRegulationMapper.insert(regulation);
    return row > 0
        ? Result.success(toResponse(regulation, club), "规章制度创建成功")
        : Result.error("规章制度创建失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Integer regulationId, RequestSaveClubRegulationDTO request) {
    ClubRegulation regulation = find(regulationId);
    if (regulation == null) return Result.error(404, "规章制度不存在");
    String status = normalizeStatus(request.getStatus(), regulation.getStatus());
    if (status == null) return Result.error(400, "制度状态只能是 draft 或 published");

    boolean firstPublish =
        "published".equals(status) && !"published".equals(regulation.getStatus());
    regulation.setTitle(request.getTitle().trim());
    regulation.setSummary(trimToNull(request.getSummary()));
    regulation.setContentMarkdown(request.getContentMarkdown().trim());
    regulation.setStatus(status);
    regulation.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    regulation.setUpdatedBy(currentUserId());
    if (firstPublish || ("published".equals(status) && regulation.getPublishedAt() == null)) {
      regulation.setPublishedAt(Times.now());
    }
    int row = clubRegulationMapper.updateById(regulation);
    return row > 0 ? Result.success("规章制度更新成功") : Result.error("规章制度更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> delete(Integer regulationId) {
    if (find(regulationId) == null) return Result.error(404, "规章制度不存在");
    int row = clubRegulationMapper.deleteById(regulationId);
    return row > 0 ? Result.success("规章制度已删除") : Result.error("规章制度删除失败");
  }

  @Override
  public Result<List<ResponseClubRegulationVO>> publicList(Integer clubId, String keyword) {
    List<ClubRegulation> rows =
        clubRegulationMapper.selectByConditions(clubId, "published", keyword);
    return Result.success(toResponses(rows, false));
  }

  @Override
  public Result<ResponseClubRegulationVO> publicDetail(Integer regulationId) {
    ClubRegulation regulation =
        regulationId == null ? null : clubRegulationMapper.selectPublishedById(regulationId);
    if (regulation == null) return Result.error(404, "规章制度不存在或尚未发布");
    return Result.success(toResponse(regulation, clubMapper.selectById(regulation.getClubId())));
  }

  private List<ResponseClubRegulationVO> toResponses(
      List<ClubRegulation> rows, boolean includeContent) {
    if (rows.isEmpty()) return List.of();
    Map<Integer, Club> clubs =
        clubMapper.selectBatchIds(
                rows.stream().map(ClubRegulation::getClubId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Club::getId, Function.identity()));
    return rows.stream()
        .map(
            regulation -> {
              ResponseClubRegulationVO response =
                  toResponse(regulation, clubs.get(regulation.getClubId()));
              if (!includeContent) response.setContentMarkdown(null);
              return response;
            })
        .toList();
  }

  private ResponseClubRegulationVO toResponse(ClubRegulation regulation, Club club) {
    return ResponseClubRegulationVO.builder()
        .id(regulation.getId())
        .clubId(regulation.getClubId())
        .clubName(club == null ? null : club.getName())
        .title(regulation.getTitle())
        .summary(regulation.getSummary())
        .contentMarkdown(regulation.getContentMarkdown())
        .status(regulation.getStatus())
        .sortOrder(regulation.getSortOrder())
        .publishedAt(regulation.getPublishedAt())
        .createdAt(regulation.getCreatedAt())
        .updatedAt(regulation.getUpdatedAt())
        .build();
  }

  private ClubRegulation find(Integer regulationId) {
    return regulationId == null ? null : clubRegulationMapper.selectById(regulationId);
  }

  private Integer currentUserId() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }

  private String normalizeStatus(String status, String fallback) {
    String normalized = trimToNull(status);
    if (normalized == null) return fallback;
    normalized = normalized.toLowerCase();
    return STATUSES.contains(normalized) ? normalized : null;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
