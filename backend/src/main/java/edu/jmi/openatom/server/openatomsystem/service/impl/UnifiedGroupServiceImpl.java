package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCheckInGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateDepartmentDTO;
import edu.jmi.openatom.server.openatomsystem.service.AlumniGroupService;
import edu.jmi.openatom.server.openatomsystem.service.CheckInService;
import edu.jmi.openatom.server.openatomsystem.service.DepartmentService;
import edu.jmi.openatom.server.openatomsystem.service.UnifiedGroupProjectionService;
import edu.jmi.openatom.server.openatomsystem.service.UnifiedGroupService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnifiedGroupServiceImpl implements UnifiedGroupService {
  private static final List<String> GROUP_TYPES =
      List.of("department", "checkin", "alumni", "external", "custom");

  private final JdbcTemplate jdbcTemplate;
  private final UnifiedGroupProjectionService projectionService;
  private final DepartmentService departmentService;
  private final CheckInService checkInService;
  private final AlumniGroupService alumniGroupService;

  @Override
  public Result<List<Map<String, Object>>> list(Integer clubId, String type, String keyword) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    if (type != null && !type.isBlank() && !GROUP_TYPES.contains(type)) {
      return Result.error(400, "分组类型不合法");
    }
    StringBuilder sql = new StringBuilder(
        """
        SELECT unified_group.id, unified_group.club_id AS clubId,
               unified_group.parent_id AS parentId, unified_group.name, unified_group.type,
               unified_group.description, unified_group.status,
               unified_group.owner_user_id AS ownerUserId,
               COALESCE(owner.real_name, owner.user_name) AS owner,
               unified_group.sort_order AS sortOrder,
               unified_group.source_type AS sourceType,
               unified_group.source_id AS legacyId,
               unified_group.config_json AS configJson,
               unified_group.created_at AS createdAt,
               unified_group.updated_at AS updatedAt,
               CASE WHEN unified_group.source_type = 'qq_group'
                    THEN COALESCE((SELECT bot_group.member_count FROM bot_group
                                   WHERE bot_group.group_id = unified_group.source_id LIMIT 1), 0)
                    ELSE (SELECT COUNT(*) FROM unified_group_member member
                          WHERE member.group_id = unified_group.id AND member.status = 'active')
               END AS memberCount
        FROM unified_group
        LEFT JOIN tb_user owner ON owner.id = unified_group.owner_user_id
        WHERE unified_group.club_id = ?
        """);
    List<Object> arguments = new ArrayList<>();
    arguments.add(clubId);
    if (type != null && !type.isBlank()) {
      sql.append(" AND unified_group.type = ?");
      arguments.add(type);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (unified_group.name LIKE ? OR unified_group.description LIKE ? OR unified_group.source_id LIKE ?)");
      String like = "%" + keyword.trim() + "%";
      arguments.add(like);
      arguments.add(like);
      arguments.add(like);
    }
    sql.append(" ORDER BY FIELD(unified_group.type, 'department', 'checkin', 'alumni', 'external', 'custom'), ")
        .append("unified_group.sort_order ASC, unified_group.name ASC, unified_group.id ASC");

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), arguments.toArray());
    rows.forEach(this::enrichGroup);
    return Result.success(rows);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> create(RequestCreateUnifiedGroupDTO request) {
    String type = request.getType() == null ? "" : request.getType().trim();
    Result<String> result = switch (type) {
      case "department" -> departmentService.createDepartment(
          request.getClubId(),
          RequestCreateDepartmentDTO.builder()
              .name(request.getName().trim())
              .description(request.getDescription())
              .build());
      case "checkin" -> {
        RequestCheckInGroupDTO checkInRequest = new RequestCheckInGroupDTO();
        checkInRequest.setName(request.getName().trim());
        checkInRequest.setUserIds(request.getUserIds());
        Result<Integer> checkInResult = checkInService.createGroup(request.getClubId(), checkInRequest);
        yield checkInResult.getCode() == Result.SUCCESS_CODE
            ? Result.success("签到分组创建成功")
            : Result.error(checkInResult.getCode(), checkInResult.getMessage());
      }
      case "alumni" -> alumniGroupService.create(
          request.getClubId(),
          RequestCreateAlumniGroupDTO.builder()
              .name(request.getName().trim())
              .description(request.getDescription())
              .sortOrder(request.getSortOrder())
              .build());
      case "external" -> Result.error(400, "外部群组请通过机器人同步创建");
      default -> Result.error(400, "分组类型不合法");
    };
    if (result.getCode() == Result.SUCCESS_CODE) {
      assignCreatedGroupMembers(type, request);
    }
    return result;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> update(Long groupId, RequestUpdateUnifiedGroupDTO request) {
    Map<String, Object> group = findGroup(groupId);
    if (group == null) return Result.error(404, "分组不存在");
    String type = String.valueOf(group.get("type"));
    Integer sourceId;
    try {
      sourceId = Integer.valueOf(String.valueOf(group.get("legacyId")));
    } catch (NumberFormatException exception) {
      return Result.error(400, "该外部分组不能在统一页面修改");
    }
    List<Integer> userIds = sanitizeUserIds(request.getUserIds());
    Result<String> result = switch (type) {
      case "department" -> departmentService.updateDepartment(
          sourceId,
          RequestUpdateDepartmentDTO.builder()
              .name(request.getName().trim())
              .description(request.getDescription())
              .build());
      case "checkin" -> {
        RequestCheckInGroupDTO checkInRequest = new RequestCheckInGroupDTO();
        checkInRequest.setName(request.getName().trim());
        checkInRequest.setUserIds(userIds);
        yield checkInService.updateGroup(sourceId, checkInRequest);
      }
      case "alumni" -> alumniGroupService.update(
          sourceId,
          RequestUpdateAlumniGroupDTO.builder()
              .name(request.getName().trim())
              .description(request.getDescription())
              .sortOrder(request.getSortOrder())
              .build());
      default -> Result.error(400, "该分组类型暂不支持统一编辑");
    };
    if (result.getCode() != Result.SUCCESS_CODE) return result;
    if (!"checkin".equals(type)) {
      replaceSourceGroupMembers(type, sourceId, String.valueOf(group.get("name")), request.getName().trim(),
          ((Number) group.get("clubId")).intValue(), userIds);
    }
    return Result.success("分组已更新");
  }

  @Override
  public Result<Map<String, Object>> userOptions(
      Integer clubId, String keyword, String status, Integer departmentId, Integer page, Integer pageSize) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    int safePage = page == null ? 1 : Math.max(1, page);
    int safePageSize = pageSize == null ? 20 : Math.min(100, Math.max(10, pageSize));
    List<Object> arguments = new ArrayList<>();
    String where = buildUserOptionWhere(clubId, keyword, status, departmentId, arguments);
    Long total = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM club_membership membership JOIN tb_user user ON user.id = membership.user_id " + where,
        Long.class,
        arguments.toArray());
    List<Object> pageArguments = new ArrayList<>(arguments);
    pageArguments.add(safePageSize);
    pageArguments.add((safePage - 1) * safePageSize);
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        """
        SELECT user.id, user.user_name AS userName, user.real_name AS realName,
               user.student_id AS studentId, user.class_name AS className, user.avatar,
               membership.status, membership.department_id AS departmentId,
               department.name AS departmentName, member_position.name AS positionName
        FROM club_membership membership
        JOIN tb_user user ON user.id = membership.user_id
        LEFT JOIN club_department department ON department.id = membership.department_id
        LEFT JOIN club_position member_position ON member_position.id = membership.position_id
        """ + where +
            " ORDER BY COALESCE(user.real_name, user.user_name), user.id LIMIT ? OFFSET ?",
        pageArguments.toArray());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", rows);
    result.put("total", total == null ? 0 : total);
    result.put("page", safePage);
    result.put("pageSize", safePageSize);
    return Result.success(result);
  }

  @Override
  public Result<List<Integer>> userOptionIds(
      Integer clubId, String keyword, String status, Integer departmentId) {
    if (clubId == null) return Result.error(400, "clubId不能为空");
    List<Object> arguments = new ArrayList<>();
    String where = buildUserOptionWhere(clubId, keyword, status, departmentId, arguments);
    return Result.success(jdbcTemplate.queryForList(
        "SELECT user.id FROM club_membership membership JOIN tb_user user ON user.id = membership.user_id "
            + where + " ORDER BY COALESCE(user.real_name, user.user_name), user.id",
        Integer.class,
        arguments.toArray()));
  }

  @Override
  public Result<Map<String, Object>> detail(Long groupId) {
    Map<String, Object> group = findGroup(groupId);
    if (group == null) return Result.error(404, "分组不存在");
    group.put("members", members(groupId).getData());
    group.put("dependencies", dependencies(groupId).getData());
    return Result.success(group);
  }

  @Override
  public Result<List<Map<String, Object>>> members(Long groupId) {
    if (!groupExists(groupId)) return Result.error(404, "分组不存在");
    return Result.success(jdbcTemplate.queryForList(
        """
        SELECT member.user_id AS userId, member.member_role AS memberRole,
               member.status, member.joined_at AS joinedAt, member.left_at AS leftAt,
               user.user_name AS userName, user.real_name AS realName,
               user.student_id AS studentId, user.avatar
        FROM unified_group_member member
        JOIN tb_user user ON user.id = member.user_id
        WHERE member.group_id = ?
        ORDER BY FIELD(member.member_role, 'owner', 'manager', 'vice_manager', 'member'),
                 COALESCE(user.real_name, user.user_name), member.user_id
        """,
        groupId));
  }

  @Override
  public Result<Map<String, Object>> dependencies(Long groupId) {
    Map<String, Object> group = findGroup(groupId);
    if (group == null) return Result.error(404, "分组不存在");
    String sourceType = String.valueOf(group.get("sourceType"));
    String legacyId = String.valueOf(group.get("legacyId"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("memberCount", count(
        "SELECT COUNT(*) FROM unified_group_member WHERE group_id = ? AND status = 'active'", groupId));
    switch (sourceType) {
      case "department" -> {
        result.put("membershipCount", count("SELECT COUNT(*) FROM club_membership WHERE department_id = ?", legacyId));
        result.put("positionCount", count("SELECT COUNT(*) FROM club_position WHERE department_id = ?", legacyId));
      }
      case "checkin" -> {
        result.put("checkInSessionCount", count("SELECT COUNT(*) FROM checkin_session WHERE group_id = ?", legacyId));
        result.put("eveningScheduleCount", count("SELECT COUNT(*) FROM evening_study_schedule WHERE group_id = ?", legacyId));
      }
      case "alumni" -> result.put(
          "membershipCount",
          count("SELECT COUNT(*) FROM club_membership WHERE alumni_group_id = ?", legacyId));
      case "qq_group" -> {
        result.put("externalMemberCount", count("SELECT COUNT(*) FROM bot_group_member WHERE group_id = ?", legacyId));
        result.put("announcementCount", count("SELECT COUNT(*) FROM bot_group_announcement WHERE group_id = ?", legacyId));
      }
      default -> {
      }
    }
    return Result.success(result);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> synchronize() {
    projectionService.synchronizeAll();
    return Result.success("统一分组数据已同步");
  }

  private Map<String, Object> findGroup(Long groupId) {
    if (groupId == null) return null;
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        """
        SELECT unified_group.id, unified_group.club_id AS clubId,
               unified_group.parent_id AS parentId, unified_group.name, unified_group.type,
               unified_group.description, unified_group.status,
               unified_group.owner_user_id AS ownerUserId,
               COALESCE(owner.real_name, owner.user_name) AS owner,
               unified_group.sort_order AS sortOrder,
               unified_group.source_type AS sourceType,
               unified_group.source_id AS legacyId,
               unified_group.config_json AS configJson,
               unified_group.created_at AS createdAt,
               unified_group.updated_at AS updatedAt,
               CASE WHEN unified_group.source_type = 'qq_group'
                    THEN COALESCE((SELECT bot_group.member_count FROM bot_group
                                   WHERE bot_group.group_id = unified_group.source_id LIMIT 1), 0)
                    ELSE (SELECT COUNT(*) FROM unified_group_member member
                          WHERE member.group_id = unified_group.id AND member.status = 'active')
               END AS memberCount
        FROM unified_group
        LEFT JOIN tb_user owner ON owner.id = unified_group.owner_user_id
        WHERE unified_group.id = ?
        """,
        groupId);
    if (rows.isEmpty()) return null;
    Map<String, Object> group = rows.getFirst();
    enrichGroup(group);
    return group;
  }

  private void enrichGroup(Map<String, Object> group) {
    String type = String.valueOf(group.get("type"));
    String sourceType = String.valueOf(group.get("sourceType"));
    String legacyId = String.valueOf(group.get("legacyId"));
    group.put("key", sourceType + ":" + legacyId);
    group.put("sourceId", sourceType + ":" + legacyId);
    group.put("userIds", jdbcTemplate.queryForList(
        "SELECT user_id FROM unified_group_member WHERE group_id = ? AND status = 'active' ORDER BY user_id",
        Number.class,
        group.get("id")));
    group.put("statusText", statusText(String.valueOf(group.get("status"))));
    group.put("bindingText", bindingText(type, String.valueOf(group.get("status"))));
  }

  private String statusText(String status) {
    return switch (status) {
      case "active" -> "正常";
      case "disabled" -> "已停用";
      case "archived" -> "已归档";
      default -> status;
    };
  }

  private String bindingText(String type, String status) {
    return switch (type) {
      case "department" -> "部门与岗位";
      case "checkin" -> "签到场次 / 晚自习";
      case "alumni" -> "往届管理人员";
      case "external" -> "active".equals(status) ? "QQ / 机器人已启用" : "QQ / 机器人未启用";
      default -> "自定义分组";
    };
  }

  private boolean groupExists(Long groupId) {
    return groupId != null && count("SELECT COUNT(*) FROM unified_group WHERE id = ?", groupId) > 0;
  }

  private String buildUserOptionWhere(
      Integer clubId, String keyword, String status, Integer departmentId, List<Object> arguments) {
    StringBuilder where = new StringBuilder(" WHERE membership.club_id = ?");
    arguments.add(clubId);
    if (keyword != null && !keyword.isBlank()) {
      where.append(
          " AND (user.user_name LIKE ? OR user.real_name LIKE ? OR user.student_id LIKE ? OR user.class_name LIKE ?)");
      String like = "%" + keyword.trim() + "%";
      arguments.add(like);
      arguments.add(like);
      arguments.add(like);
      arguments.add(like);
    }
    if (status != null && !status.isBlank()) {
      where.append(" AND membership.status = ?");
      arguments.add(status.trim());
    }
    if (departmentId != null) {
      where.append(" AND membership.department_id = ?");
      arguments.add(departmentId);
    }
    return where.toString();
  }

  private void assignCreatedGroupMembers(String type, RequestCreateUnifiedGroupDTO request) {
    List<Integer> userIds = sanitizeUserIds(request.getUserIds());
    if (userIds.isEmpty() || "checkin".equals(type)) return;

    String table = "department".equals(type) ? "club_department" : "club_alumni_group";
    List<Integer> sourceIds = jdbcTemplate.queryForList(
        "SELECT id FROM " + table + " WHERE club_id = ? AND name = ? ORDER BY id DESC LIMIT 1",
        Integer.class,
        request.getClubId(),
        request.getName().trim());
    if (sourceIds.isEmpty()) return;

    String placeholders = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
    List<Object> arguments = new ArrayList<>();
    if ("department".equals(type)) {
      arguments.add(sourceIds.getFirst());
      arguments.add(request.getClubId());
      arguments.addAll(userIds);
      jdbcTemplate.update(
          "UPDATE club_membership SET department_id = ?, position_id = NULL WHERE club_id = ? AND user_id IN ("
              + placeholders + ")",
          arguments.toArray());
    } else if ("alumni".equals(type)) {
      arguments.add(sourceIds.getFirst());
      arguments.add(request.getName().trim());
      arguments.add(request.getClubId());
      arguments.addAll(userIds);
      jdbcTemplate.update(
          "UPDATE club_membership SET alumni_group_id = ?, alumni_group = ? WHERE club_id = ? AND user_id IN ("
              + placeholders + ")",
          arguments.toArray());
    }
    projectionService.syncClubMemberships(request.getClubId());
  }

  private List<Integer> sanitizeUserIds(List<Integer> userIds) {
    return userIds == null
        ? List.of()
        : new ArrayList<>(new LinkedHashSet<>(userIds)).stream()
            .filter(id -> id != null && id > 0)
            .toList();
  }

  private void replaceSourceGroupMembers(
      String type, Integer sourceId, String oldName, String newName, Integer clubId, List<Integer> userIds) {
    if ("department".equals(type)) {
      jdbcTemplate.update(
          "UPDATE club_membership SET department_id = NULL, position_id = NULL WHERE club_id = ? AND department_id = ?",
          clubId,
          sourceId);
      updateMembershipSelection(
          "UPDATE club_membership SET department_id = ?, position_id = NULL WHERE club_id = ? AND user_id IN (%s)",
          sourceId,
          null,
          clubId,
          userIds);
    } else if ("alumni".equals(type)) {
      jdbcTemplate.update(
          "UPDATE club_membership SET alumni_group_id = NULL, alumni_group = NULL WHERE club_id = ? AND (alumni_group_id = ? OR alumni_group = ?)",
          clubId,
          sourceId,
          oldName);
      updateMembershipSelection(
          "UPDATE club_membership SET alumni_group_id = ?, alumni_group = ? WHERE club_id = ? AND user_id IN (%s)",
          sourceId,
          newName,
          clubId,
          userIds);
    }
    projectionService.syncClubMemberships(clubId);
  }

  private void updateMembershipSelection(
      String sqlTemplate, Integer sourceId, String groupName, Integer clubId, List<Integer> userIds) {
    if (userIds.isEmpty()) return;
    String placeholders = String.join(",", java.util.Collections.nCopies(userIds.size(), "?"));
    List<Object> arguments = new ArrayList<>();
    arguments.add(sourceId);
    if (groupName != null) arguments.add(groupName);
    arguments.add(clubId);
    arguments.addAll(userIds);
    jdbcTemplate.update(String.format(sqlTemplate, placeholders), arguments.toArray());
  }

  private long count(String sql, Object... arguments) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
    return value == null ? 0 : value;
  }
}
