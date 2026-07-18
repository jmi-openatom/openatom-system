package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroup;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.service.UnifiedGroupProjectionService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnifiedGroupProjectionServiceImpl implements UnifiedGroupProjectionService {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void syncDepartment(ClubDepartment department) {
    if (department == null || department.getId() == null) return;
    upsertGroup(
        department.getClubId(),
        department.getName(),
        "department",
        department.getDescription(),
        "active",
        department.getManagerUserId(),
        0,
        "department",
        department.getId(),
        null);
    replaceBinding("department", department.getId(), "department", department.getId(), null);
    syncClubMemberships(department.getClubId());
  }

  @Override
  public void syncCheckInGroup(CheckInGroup group) {
    if (group == null || group.getId() == null) return;
    upsertGroup(
        group.getClubId(), group.getName(), "checkin", null, "active", null, 0,
        "checkin", group.getId(), null);
    replaceBinding("checkin", group.getId(), "checkin", group.getId(), null);
    Long unifiedGroupId = findUnifiedGroupId("checkin", group.getId());
    if (unifiedGroupId == null) return;
    jdbcTemplate.update("DELETE FROM unified_group_member WHERE group_id = ?", unifiedGroupId);
    jdbcTemplate.update(
        """
        INSERT INTO unified_group_member (group_id, user_id, member_role, status, joined_at)
        SELECT ?, member.user_id, 'member', 'active', member.created_at
        FROM checkin_group_member member
        WHERE member.group_id = ?
        """,
        unifiedGroupId,
        group.getId());
  }

  @Override
  public void syncAlumniGroup(ClubAlumniGroup group) {
    if (group == null || group.getId() == null) return;
    upsertGroup(
        group.getClubId(), group.getName(), "alumni", group.getDescription(), "active", null,
        group.getSortOrder() == null ? 0 : group.getSortOrder(), "alumni", group.getId(), null);
    replaceBinding("alumni", group.getId(), "alumni", group.getId(), null);
    syncClubMemberships(group.getClubId());
  }

  @Override
  public void syncClubMemberships(Integer clubId) {
    if (clubId == null) return;
    jdbcTemplate.update(
        """
        DELETE member
        FROM unified_group_member member
        JOIN unified_group unified_group ON unified_group.id = member.group_id
        WHERE unified_group.club_id = ? AND unified_group.type IN ('department', 'alumni')
        """,
        clubId);
    jdbcTemplate.update(
        """
        INSERT INTO unified_group_member
            (group_id, user_id, member_role, status, joined_at, left_at)
        SELECT unified_group.id, membership.user_id,
               CASE
                 WHEN department.manager_user_id = membership.user_id THEN 'manager'
                 WHEN department.vice_manager_user_id = membership.user_id THEN 'vice_manager'
                 ELSE 'member'
               END,
               IF(membership.left_at IS NULL, 'active', 'inactive'), membership.joined_at, membership.left_at
        FROM club_membership membership
        JOIN club_department department ON department.id = membership.department_id
        JOIN unified_group unified_group
          ON unified_group.source_type = 'department'
         AND unified_group.source_id = CAST(department.id AS CHAR)
        WHERE membership.club_id = ?
        ON DUPLICATE KEY UPDATE
          member_role = VALUES(member_role), status = VALUES(status),
          joined_at = VALUES(joined_at), left_at = VALUES(left_at)
        """,
        clubId);
    jdbcTemplate.update(
        """
        INSERT INTO unified_group_member
            (group_id, user_id, member_role, status, joined_at, left_at)
        SELECT unified_group.id, membership.user_id, 'member', 'active', membership.joined_at, membership.left_at
        FROM club_membership membership
        JOIN unified_group unified_group
          ON unified_group.source_type = 'alumni'
         AND unified_group.source_id = CAST(COALESCE(
               membership.alumni_group_id,
               (SELECT alumni_group.id
                FROM club_alumni_group alumni_group
                WHERE alumni_group.club_id = membership.club_id
                  AND alumni_group.name = membership.alumni_group
                LIMIT 1)
             ) AS CHAR)
        WHERE membership.club_id = ?
          AND (membership.alumni_group_id IS NOT NULL OR membership.alumni_group IS NOT NULL)
        ON DUPLICATE KEY UPDATE
          status = VALUES(status), joined_at = VALUES(joined_at), left_at = VALUES(left_at)
        """,
        clubId);
  }

  @Override
  public void syncBotGroup(String groupId) {
    if (groupId == null || groupId.isBlank()) return;
    List<Map<String, Object>> groups = jdbcTemplate.queryForList(
        """
        SELECT bot_group.group_id, bot_group.group_name, bot_group.bot_enabled, bot_group.mode,
               bot_group.created_at,
               (SELECT COALESCE(MAX(CASE WHEN club.code = 'JMI-OPENATOM' THEN club.id END), MIN(club.id))
                FROM club) AS club_id
        FROM bot_group bot_group
        WHERE bot_group.group_id = ?
        """,
        groupId);
    if (groups.isEmpty()) return;
    Map<String, Object> group = groups.getFirst();
    Integer clubId = ((Number) group.get("club_id")).intValue();
    String groupName = text(group.get("group_name"));
    boolean enabled = booleanValue(group.get("bot_enabled"));
    String mode = text(group.get("mode"));
    upsertGroup(
        clubId,
        groupName == null ? "QQ群 " + groupId : groupName,
        "external",
        "QQ群号 " + groupId,
        enabled ? "active" : "disabled",
        null,
        0,
        "qq_group",
        groupId,
        "{\"platform\":\"qq\",\"mode\":\"" + escapeJson(mode == null ? "enabled" : mode) + "\"}");
    replaceBinding("qq_group", groupId, "qq_group", groupId, "{\"platform\":\"qq\"}");
  }

  @Override
  public void syncBotGroups() {
    jdbcTemplate.queryForList("SELECT group_id FROM bot_group")
        .forEach(row -> syncBotGroup(text(row.get("group_id"))));
  }

  @Override
  public void removeSource(String sourceType, Object sourceId) {
    if (sourceType == null || sourceId == null) return;
    jdbcTemplate.update(
        "DELETE FROM unified_group WHERE source_type = ? AND source_id = ?",
        sourceType,
        String.valueOf(sourceId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void synchronizeAll() {
    jdbcTemplate.queryForList("SELECT id FROM club_department")
        .forEach(row -> syncDepartment(loadDepartment(number(row.get("id")))));
    jdbcTemplate.queryForList("SELECT id FROM checkin_group")
        .forEach(row -> syncCheckInGroup(loadCheckInGroup(number(row.get("id")))));
    jdbcTemplate.queryForList("SELECT id FROM club_alumni_group")
        .forEach(row -> syncAlumniGroup(loadAlumniGroup(number(row.get("id")))));
    syncBotGroups();
    jdbcTemplate.queryForList("SELECT id FROM club")
        .forEach(row -> syncClubMemberships(number(row.get("id"))));
    removeStaleSources();
  }

  private void upsertGroup(
      Integer clubId,
      String name,
      String type,
      String description,
      String status,
      Integer ownerUserId,
      Integer sortOrder,
      String sourceType,
      Object sourceId,
      String configJson) {
    jdbcTemplate.update(
        """
        INSERT INTO unified_group
          (club_id, name, type, description, status, owner_user_id, sort_order,
           source_type, source_id, config_json)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          club_id = VALUES(club_id), name = VALUES(name), type = VALUES(type),
          description = VALUES(description), status = VALUES(status),
          owner_user_id = VALUES(owner_user_id), sort_order = VALUES(sort_order),
          config_json = VALUES(config_json), updated_at = CURRENT_TIMESTAMP
        """,
        clubId,
        name,
        type,
        description,
        status,
        ownerUserId,
        sortOrder,
        sourceType,
        String.valueOf(sourceId),
        configJson);
  }

  private void removeStaleSources() {
    jdbcTemplate.update(
        """
        DELETE unified_group FROM unified_group
        WHERE unified_group.source_type = 'department'
          AND NOT EXISTS (SELECT 1 FROM club_department source
                          WHERE CAST(source.id AS CHAR) = unified_group.source_id)
        """);
    jdbcTemplate.update(
        """
        DELETE unified_group FROM unified_group
        WHERE unified_group.source_type = 'checkin'
          AND NOT EXISTS (SELECT 1 FROM checkin_group source
                          WHERE CAST(source.id AS CHAR) = unified_group.source_id)
        """);
    jdbcTemplate.update(
        """
        DELETE unified_group FROM unified_group
        WHERE unified_group.source_type = 'alumni'
          AND NOT EXISTS (SELECT 1 FROM club_alumni_group source
                          WHERE CAST(source.id AS CHAR) = unified_group.source_id)
        """);
    jdbcTemplate.update(
        """
        DELETE unified_group FROM unified_group
        WHERE unified_group.source_type = 'qq_group'
          AND NOT EXISTS (SELECT 1 FROM bot_group source
                          WHERE source.group_id = unified_group.source_id)
        """);
  }

  private void replaceBinding(
      String sourceType, Object sourceId, String bindingType, Object externalId, String configJson) {
    Long groupId = findUnifiedGroupId(sourceType, sourceId);
    if (groupId == null) return;
    jdbcTemplate.update(
        """
        INSERT INTO unified_group_binding (group_id, binding_type, external_id, config_json)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE status = 'active', config_json = VALUES(config_json),
                                updated_at = CURRENT_TIMESTAMP
        """,
        groupId,
        bindingType,
        String.valueOf(externalId),
        configJson);
  }

  private Long findUnifiedGroupId(String sourceType, Object sourceId) {
    List<Long> ids = jdbcTemplate.query(
        "SELECT id FROM unified_group WHERE source_type = ? AND source_id = ? LIMIT 1",
        (resultSet, rowNum) -> resultSet.getLong(1),
        sourceType,
        String.valueOf(sourceId));
    return ids.isEmpty() ? null : ids.getFirst();
  }

  private ClubDepartment loadDepartment(Integer id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, club_id, name, description, manager_user_id, vice_manager_user_id,
               wechat_group_qrcode
        FROM club_department WHERE id = ?
        """,
        (rs, rowNum) -> ClubDepartment.builder()
            .id(rs.getInt("id"))
            .clubId(rs.getInt("club_id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .managerUserId((Integer) rs.getObject("manager_user_id"))
            .viceManagerUserId((Integer) rs.getObject("vice_manager_user_id"))
            .wechatGroupQrcode(rs.getString("wechat_group_qrcode"))
            .build(),
        id);
  }

  private CheckInGroup loadCheckInGroup(Integer id) {
    return jdbcTemplate.queryForObject(
        "SELECT id, club_id, name, created_by FROM checkin_group WHERE id = ?",
        (rs, rowNum) -> CheckInGroup.builder()
            .id(rs.getInt("id"))
            .clubId(rs.getInt("club_id"))
            .name(rs.getString("name"))
            .createdBy((Integer) rs.getObject("created_by"))
            .build(),
        id);
  }

  private ClubAlumniGroup loadAlumniGroup(Integer id) {
    return jdbcTemplate.queryForObject(
        "SELECT id, club_id, name, description, sort_order FROM club_alumni_group WHERE id = ?",
        (rs, rowNum) -> ClubAlumniGroup.builder()
            .id(rs.getInt("id"))
            .clubId(rs.getInt("club_id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .sortOrder(rs.getInt("sort_order"))
            .build(),
        id);
  }

  private Integer number(Object value) {
    return value instanceof Number number ? number.intValue() : Integer.valueOf(String.valueOf(value));
  }

  private String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private boolean booleanValue(Object value) {
    if (value instanceof Boolean bool) return bool;
    if (value instanceof Number number) return number.intValue() != 0;
    return Boolean.parseBoolean(text(value));
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
