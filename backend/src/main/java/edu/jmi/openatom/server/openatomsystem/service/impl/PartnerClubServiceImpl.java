package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSavePartnerClubDTO;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.PartnerClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.PartnerClubService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePartnerClubVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePartnerClubUserOptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 开源伙伴管理与公开展示。 */
@Service
@RequiredArgsConstructor
public class PartnerClubServiceImpl implements PartnerClubService {
  private static final String STATUS_DRAFT = "draft";
  private static final String STATUS_PUBLISHED = "published";
  private static final String STATUS_DISABLED = "disabled";

  private final PartnerClubMapper partnerClubMapper;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;

  @Override
  public Result<List<ResponsePartnerClubVO>> publicList(Boolean featured, Integer limit) {
    if (limit != null && (limit < 1 || limit > 100)) {
      return Result.error(400, "limit 必须在 1 到 100 之间");
    }
    List<PartnerClub> rows =
        partnerClubMapper.selectPublished(featured, limit).stream()
            .filter(
                club ->
                    trimToNull(club.getWebsiteUrl()) == null
                        || isSafeWebsite(club.getWebsiteUrl()))
            .toList();
    Map<Integer, User> presidents = presidentUsers(rows);
    List<ResponsePartnerClubVO> clubs =
        rows.stream()
            .map(club -> toResponse(club, presidentUser(presidents, club.getPresidentUserId())))
            .toList();
    return Result.success(clubs);
  }

  @Override
  public Result<PageDataVO<PartnerClub>> adminList(
      String keyword, String status, Boolean featured, Long page, Long pageSize) {
    Page<PartnerClub> result =
        partnerClubMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            trimToNull(keyword),
            normalizeStatus(status),
            featured);
    enrichPresidents(result.getRecords());
    return Result.success(
        PageDataVO.<PartnerClub>builder()
            .list(result.getRecords())
            .page(result.getCurrent())
            .pageSize(result.getSize())
            .total(result.getTotal())
            .build());
  }

  @Override
  public Result<List<ResponsePartnerClubUserOptionVO>> userOptions(String keyword, Integer limit) {
    if (limit != null && (limit < 1 || limit > 100)) {
      return Result.error(400, "limit 必须在 1 到 100 之间");
    }
    List<ResponsePartnerClubUserOptionVO> options =
        userMapper.selectPartnerClubOptions(trimToNull(keyword), limit).stream()
            .map(
                user ->
                    ResponsePartnerClubUserOptionVO.builder()
                        .id(user.getId())
                        .userName(user.getUserName())
                        .realName(user.getRealName())
                        .studentId(user.getStudentId())
                        .avatar(user.getAvatar())
                        .build())
            .toList();
    return Result.success(options);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<PartnerClub> create(RequestSavePartnerClubDTO request) {
    PartnerClub club = new PartnerClub();
    applyRequest(club, request);
    club.setCreatedAt(Times.now());
    partnerClubMapper.insert(club);
    return Result.success(club, "开源伙伴已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<PartnerClub> update(Integer partnerClubId, RequestSavePartnerClubDTO request) {
    PartnerClub club = partnerClubId == null ? null : partnerClubMapper.selectById(partnerClubId);
    if (club == null) return Result.error(404, "开源伙伴不存在");
    applyRequest(club, request);
    partnerClubMapper.updateById(club);
    return Result.success(club, "开源伙伴已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<PartnerClub> updateStatus(Integer partnerClubId, String status) {
    PartnerClub club = partnerClubId == null ? null : partnerClubMapper.selectById(partnerClubId);
    if (club == null) return Result.error(404, "开源伙伴不存在");
    club.setStatus(requiredStatus(status));
    partnerClubMapper.updateById(club);
    return Result.success(club, "发布状态已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> delete(Integer partnerClubId) {
    if (partnerClubId == null || partnerClubMapper.selectById(partnerClubId) == null) {
      return Result.error(404, "开源伙伴不存在");
    }
    partnerClubMapper.deleteById(partnerClubId);
    return Result.success("开源伙伴已删除");
  }

  private void applyRequest(PartnerClub club, RequestSavePartnerClubDTO request) {
    if (request == null) throw new IllegalArgumentException("伙伴信息不能为空");
    String websiteUrl = trimToNull(request.getWebsiteUrl());
    if (websiteUrl != null && !isSafeWebsite(websiteUrl)) {
      throw new IllegalArgumentException("官网地址仅支持 http 或 https");
    }
    String logoUrl = required(request.getLogoUrl(), "Logo 地址不能为空");
    if (!isSafeLogo(logoUrl)) throw new IllegalArgumentException("Logo 地址必须是站内路径或 http/https 地址");
    Integer presidentUserId = request.getPresidentUserId();
    if (presidentUserId != null && userMapper.selectById(presidentUserId) == null) {
      throw new IllegalArgumentException("绑定的社长用户不存在");
    }
    club.setName(required(request.getName(), "伙伴名称不能为空"));
    club.setLogoUrl(logoUrl);
    club.setDescription(required(request.getDescription(), "伙伴简介不能为空"));
    club.setWebsiteUrl(websiteUrl);
    club.setOrganization(trimToNull(request.getOrganization()));
    club.setCategory(trimToNull(request.getCategory()));
    club.setPresidentUserId(presidentUserId);
    try {
      club.setTags(objectMapper.writeValueAsString(request.getTags() == null ? List.of() : request.getTags()));
    } catch (Exception error) {
      throw new IllegalArgumentException("标签格式无效", error);
    }
    club.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    club.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
    club.setStatus(requiredStatus(request.getStatus() == null ? STATUS_DRAFT : request.getStatus()));
  }

  private ResponsePartnerClubVO toResponse(PartnerClub club, User president) {
    return ResponsePartnerClubVO.builder()
        .id(club.getId())
        .name(club.getName())
        .logoUrl(club.getLogoUrl())
        .description(club.getDescription())
        .websiteUrl(
            isSafeWebsite(club.getWebsiteUrl()) ? club.getWebsiteUrl().trim() : null)
        .organization(club.getOrganization())
        .category(club.getCategory())
        .presidentName(president == null ? null : displayName(president))
        .presidentAvatarUrl(president == null ? null : trimToNull(president.getAvatar()))
        .tags(parseTags(club.getTags()))
        .sortOrder(club.getSortOrder())
        .featured(club.getFeatured())
        .build();
  }

  private void enrichPresidents(List<PartnerClub> clubs) {
    Map<Integer, User> presidents = presidentUsers(clubs);
    for (PartnerClub club : clubs) {
      User president = presidentUser(presidents, club.getPresidentUserId());
      club.setPresidentName(president == null ? null : displayName(president));
      club.setPresidentAvatarUrl(president == null ? null : trimToNull(president.getAvatar()));
    }
  }

  private User presidentUser(Map<Integer, User> presidents, Integer presidentUserId) {
    return presidentUserId == null ? null : presidents.get(presidentUserId);
  }

  private Map<Integer, User> presidentUsers(List<PartnerClub> clubs) {
    LinkedHashSet<Integer> userIds =
        clubs.stream()
            .map(PartnerClub::getPresidentUserId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (userIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
  }

  private String displayName(User user) {
    String realName = trimToNull(user.getRealName());
    return realName == null ? user.getUserName() : realName;
  }

  private List<String> parseTags(String rawTags) {
    if (rawTags == null || rawTags.isBlank()) return Collections.emptyList();
    try {
      return objectMapper.readValue(rawTags, new TypeReference<>() {});
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  private boolean isSafeWebsite(String url) {
    if (url == null || url.isBlank()) return false;
    try {
      String scheme = URI.create(url.trim()).getScheme();
      return "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private boolean isSafeLogo(String url) {
    return url != null && ((url.startsWith("/") && !url.startsWith("//")) || isSafeWebsite(url));
  }

  private String requiredStatus(String status) {
    String normalized = normalizeStatus(status);
    if (STATUS_DRAFT.equals(normalized)
        || STATUS_PUBLISHED.equals(normalized)
        || STATUS_DISABLED.equals(normalized)) return normalized;
    throw new IllegalArgumentException("状态只能是 draft、published 或 disabled");
  }

  private String normalizeStatus(String value) {
    String trimmed = trimToNull(value);
    return trimmed == null ? null : trimmed.toLowerCase();
  }

  private String required(String value, String message) {
    String trimmed = trimToNull(value);
    if (trimmed == null) throw new IllegalArgumentException(message);
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
