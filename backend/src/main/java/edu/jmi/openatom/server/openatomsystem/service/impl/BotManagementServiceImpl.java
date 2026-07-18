package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.BotManagementService;
import edu.jmi.openatom.server.openatomsystem.service.UnifiedGroupProjectionService;
import edu.jmi.openatom.server.openatomsystem.service.impl.NapCatClient.NapCatResponse;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotManagementServiceImpl implements BotManagementService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final int GROUP_NOTICE_MAX_TEXT_LENGTH = 600;
  private static final List<String> BOT_MODES =
      List.of("enabled", "disabled", "admin_only", "command_only", "silent");

  private final JdbcTemplate jdbcTemplate;
  private final NapCatClient napCatClient;
  private final ObjectMapper objectMapper;
  private final UnifiedGroupProjectionService unifiedGroupProjectionService;

  @Override
  public Result<Map<String, Object>> overview() {
    NapCatResponse login = napCatClient.getLoginInfo();
    Map<String, Object> overview = new LinkedHashMap<>();
    overview.put("napCatOnline", login.ok());
    overview.put("napCatMessage", login.ok() ? "已连接" : login.message());
    overview.put("loginInfo", login.data());
    overview.put("astrBotStatus", "由 AstrBot 原有服务保持运行，当前后台不接管既有插件。");
    overview.put("groupCount", count("SELECT COUNT(*) FROM bot_group"));
    overview.put("enabledGroupCount", count("SELECT COUNT(*) FROM bot_group WHERE bot_enabled = 1"));
    overview.put("memberCount", count("SELECT COUNT(*) FROM bot_group_member"));
    overview.put("pendingJoinRequestCount", count("SELECT COUNT(*) FROM bot_join_request WHERE status = 'pending'"));
    overview.put("failedAnnouncementCount", count("SELECT COUNT(*) FROM bot_group_announcement WHERE status = 'failed'"));
    overview.put("todayMessageCount", count("SELECT COALESCE(SUM(message_count), 0) FROM bot_message_stat WHERE stat_date = CURDATE()"));
    overview.put(
        "todayOperationCount",
        count(
            "SELECT COUNT(*) FROM operation_log WHERE module = 'bot_group_management' AND DATE(created_at) = CURDATE()"));
    return Result.success(overview);
  }

  @Override
  public Result<List<Map<String, Object>>> accounts() {
    return Result.success(
        queryForList(
            """
            SELECT id, account_id AS accountId, nickname, platform, status, api_base_url AS apiBaseUrl,
                   enabled, last_seen_at AS lastSeenAt, created_at AS createdAt, updated_at AS updatedAt
            FROM bot_account
            ORDER BY id ASC
            """));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> handleOneBotEvent(Map<String, Object> request) {
    String postType = trimToNull(firstPresent(request, "post_type", "postType"));
    String groupId = idString(firstPresent(request, "group_id", "groupId"));
    if (groupId != null) {
      ensureMinimalGroup(groupId);
      unifiedGroupProjectionService.syncBotGroup(groupId);
    }
    if (List.of("message", "message_sent").contains(postType) && groupId != null) {
      String messageType = trimToNull(firstPresent(request, "message_type", "messageType"));
      if ("group".equals(messageType)) {
        String rawMessage = text(firstPresent(request, "raw_message", "rawMessage", "message"));
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("groupId", groupId);
        stat.put("userId", idString(firstPresent(request, "user_id", "userId", "sender_id", "senderId")));
        stat.put("messageCount", 1);
        stat.put("commandCount", isBotCommand(rawMessage) ? 1 : 0);
        recordMessageStat(stat);
        return Result.success("群消息事件已记录");
      }
    }
    if ("request".equals(postType) || firstPresent(request, "request_type", "requestType") != null) {
      String requestType = trimToNull(firstPresent(request, "request_type", "requestType"));
      if ("friend".equals(requestType)) {
        return autoApproveFriendRequest(request);
      }
    }
    if (groupId != null && ("request".equals(postType) || firstPresent(request, "request_type", "requestType") != null)) {
      String requestType = trimToNull(firstPresent(request, "request_type", "requestType"));
      String requestSubType = trimToNull(firstPresent(request, "sub_type", "subType"));
      if ("group".equals(requestType) && (requestSubType == null || "add".equals(requestSubType))) {
        String flag = trimToNull(request.get("flag"));
        Map<String, Object> joinRequest = new LinkedHashMap<>();
        joinRequest.put("requestId", firstNonBlank(trimToNull(firstPresent(request, "request_id", "requestId")), flag));
        joinRequest.put("flag", flag);
        joinRequest.put("userId", idString(firstPresent(request, "user_id", "userId")));
        joinRequest.put("nickname", eventNickname(request));
        joinRequest.put("comment", trimToNull(firstPresent(request, "comment", "message", "request_msg", "requestMsg")));
        joinRequest.put("requestedAt", timestampFromValue(firstPresent(request, "time", "timestamp", "requested_at", "requestedAt")));
        saveJoinRequest(groupId, joinRequest);
        applyAutoReviewIfMatched(groupId, joinRequest);
        logOperation("入群申请", "记录申请", groupId + ":" + firstNonBlank(flag, idString(joinRequest.get("userId"))), text(joinRequest.get("comment")));
        return Result.success("入群申请事件已记录");
      }
    }
    if ("notice".equals(postType) && groupId != null) {
      String noticeType = trimToNull(firstPresent(request, "notice_type", "noticeType"));
      if ("group_increase".equals(noticeType)) {
        String userId = idString(firstPresent(request, "user_id", "userId"));
        upsertJoinedMember(groupId, userId);
        sendWelcomeIfEnabled(groupId, userId, eventNickname(request));
        return Result.success("新成员入群事件已处理");
      }
    }
    return Result.success("事件已忽略");
  }

  private Result<String> autoApproveFriendRequest(Map<String, Object> request) {
    String flag = trimToNull(request.get("flag"));
    String userId = idString(firstPresent(request, "user_id", "userId"));
    String nickname = eventNickname(request);
    String comment = trimToNull(firstPresent(request, "comment", "message", "request_msg", "requestMsg"));
    if (flag == null) {
      logOperation("好友申请", "自动同意失败", firstNonBlank(userId, "unknown"), "缺少 NapCat flag");
      return Result.success("好友申请缺少 NapCat flag，已忽略");
    }
    String remark = firstNonBlank(nickname, userId);
    NapCatResponse response = napCatClient.handleFriendRequest(flag, true, remark);
    String targetId = firstNonBlank(userId, flag);
    logOperation(
        "好友申请",
        response.ok() ? "自动同意" : "自动同意失败",
        targetId,
        firstNonBlank(comment, response.message()));
    if (!response.ok()) {
      return Result.error(502, "自动同意好友申请失败：" + response.message());
    }
    return Result.success("好友申请已自动同意");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> saveAccount(Map<String, Object> request) {
    String accountId = trimToNull(request.get("accountId"));
    if (accountId == null) {
      return Result.error(400, "请填写机器人账号");
    }
    Integer id = toInteger(request.get("id"), null);
    String nickname = trimToNull(request.get("nickname"));
    String platform = trimToNull(request.getOrDefault("platform", "qq"));
    String apiBaseUrl = trimToNull(request.get("apiBaseUrl"));
    boolean enabled = toBoolean(request.get("enabled"), true);
    if (id == null) {
      jdbcTemplate.update(
          """
          INSERT INTO bot_account (account_id, nickname, platform, status, api_base_url, enabled)
          VALUES (?, ?, ?, 'unknown', ?, ?)
          ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), platform = VALUES(platform),
                                  api_base_url = VALUES(api_base_url), enabled = VALUES(enabled)
          """,
          accountId,
          nickname,
          platform,
          apiBaseUrl,
          enabled);
    } else {
      jdbcTemplate.update(
          """
          UPDATE bot_account
          SET account_id = ?, nickname = ?, platform = ?, api_base_url = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
          """,
          accountId,
          nickname,
          platform,
          apiBaseUrl,
          enabled,
          id);
    }
    logOperation("账号配置", "保存机器人账号", accountId, "保存机器人账号：" + accountId);
    return Result.success("机器人账号已保存");
  }

  @Override
  public Result<List<Map<String, Object>>> groups(String keyword, Boolean botEnabled, String mode) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT g.id, g.bot_account_id AS botAccountId, g.group_id AS groupId, g.group_name AS groupName,
                   g.owner_id AS ownerId, g.owner_nickname AS ownerNickname, g.member_count AS memberCount,
                   g.admin_count AS adminCount, g.bot_role AS botRole, g.bot_enabled AS botEnabled,
                   g.mode, g.last_active_at AS lastActiveAt, g.last_synced_at AS lastSyncedAt,
                   c.welcome_enabled AS welcomeEnabled, c.auto_review_enabled AS autoReviewEnabled,
                   c.sensitive_filter_enabled AS sensitiveFilterEnabled
            FROM bot_group g
            LEFT JOIN bot_group_config c ON c.group_id = g.group_id
            WHERE 1 = 1
            """);
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (g.group_id LIKE ? OR g.group_name LIKE ? OR g.owner_id LIKE ?)");
      String like = "%" + keyword.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (botEnabled != null) {
      sql.append(" AND g.bot_enabled = ?");
      args.add(botEnabled);
    }
    if (mode != null && !mode.isBlank()) {
      sql.append(" AND g.mode = ?");
      args.add(mode.trim());
    }
    sql.append(" ORDER BY COALESCE(g.last_active_at, g.updated_at, g.created_at) DESC, g.id DESC");
    return Result.success(queryForList(sql.toString(), args.toArray()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> syncGroups() {
    NapCatResponse response = napCatClient.getGroupList();
    if (!response.ok()) {
      return Result.error(502, "同步群列表失败：" + response.message());
    }
    List<Map<String, Object>> groups = asMapList(response.data());
    int changed = 0;
    for (Map<String, Object> item : groups) {
      String groupId = idString(firstPresent(item, "group_id", "groupId"));
      if (groupId == null) {
        continue;
      }
      String groupName = text(firstPresent(item, "group_name", "groupName", "name"));
      Integer memberCount = toInteger(firstPresent(item, "member_count", "memberCount"), 0);
      jdbcTemplate.update(
          """
          INSERT INTO bot_group (bot_account_id, group_id, group_name, member_count, last_synced_at)
          VALUES (1, ?, ?, ?, CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE group_name = VALUES(group_name), member_count = VALUES(member_count),
                                  last_synced_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
          """,
          groupId,
          groupName,
          memberCount);
      ensureGroupConfig(groupId);
      changed++;
    }
    unifiedGroupProjectionService.syncBotGroups();
    logOperation("群列表", "同步群列表", "all", "从 NapCat 同步 QQ 群：" + changed + " 个");
    return Result.success(Map.of("syncedCount", changed), "群列表已同步");
  }

  @Override
  public Result<Map<String, Object>> groupDetail(String groupId) {
    Map<String, Object> group = findGroup(groupId);
    if (group == null) {
      return Result.error(404, "群聊不存在，请先同步群列表");
    }
    ensureGroupConfig(groupId);
    group.put("config", findConfig(groupId));
    group.put("announcements", announcements(groupId).getData());
    group.put("pendingJoinRequests", joinRequests(groupId, "pending").getData());
    group.put("recentStats", statisticsByGroup(groupId, LocalDate.now(ZONE).minusDays(13), LocalDate.now(ZONE)));
    return Result.success(group);
  }

  @Override
  public Result<List<Map<String, Object>>> members(String groupId, String keyword, String role, String muteStatus) {
    if (findGroup(groupId) == null) {
      return Result.error(404, "群聊不存在");
    }
    List<Object> args = new ArrayList<>();
    args.add(groupId);
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT id, group_id AS groupId, user_id AS userId, nickname, card, role,
                   join_time AS joinTime, last_sent_time AS lastSentTime, muted_until AS mutedUntil,
                   CASE WHEN muted_until IS NOT NULL AND muted_until > CURRENT_TIMESTAMP THEN 1 ELSE 0 END AS muted,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM bot_group_member
            WHERE group_id = ?
            """);
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" AND (user_id LIKE ? OR nickname LIKE ? OR card LIKE ?)");
      String like = "%" + keyword.trim() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (role != null && !role.isBlank()) {
      sql.append(" AND role = ?");
      args.add(role.trim());
    }
    if ("muted".equals(muteStatus)) {
      sql.append(" AND muted_until IS NOT NULL AND muted_until > CURRENT_TIMESTAMP");
    } else if ("normal".equals(muteStatus)) {
      sql.append(" AND (muted_until IS NULL OR muted_until <= CURRENT_TIMESTAMP)");
    }
    sql.append(" ORDER BY FIELD(role, 'owner', 'admin', 'member'), CONVERT(COALESCE(card, nickname, user_id) USING gbk)");
    return Result.success(queryForList(sql.toString(), args.toArray()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> syncMembers(String groupId) {
    Map<String, Object> group = findGroup(groupId);
    if (group == null) {
      return Result.error(404, "群聊不存在，请先同步群列表");
    }
    NapCatResponse response = napCatClient.getGroupMemberList(groupId);
    if (!response.ok()) {
      return Result.error(502, "同步群成员失败：" + response.message());
    }
    List<Map<String, Object>> members = asMapList(response.data());
    int adminCount = 0;
    String ownerId = null;
    String ownerNickname = null;
    for (Map<String, Object> item : members) {
      String userId = idString(firstPresent(item, "user_id", "userId"));
      if (userId == null) {
        continue;
      }
      String role = normalizeRole(text(firstPresent(item, "role")));
      if ("owner".equals(role)) {
        ownerId = userId;
        ownerNickname = text(firstPresent(item, "nickname", "card"));
      }
      if ("admin".equals(role)) {
        adminCount++;
      }
      Timestamp joinTime = oneBotTimestamp(firstPresent(item, "join_time", "joinTime"));
      Timestamp lastSentTime = oneBotTimestamp(firstPresent(item, "last_sent_time", "lastSentTime"));
      Timestamp mutedUntil = oneBotTimestamp(firstPresent(item, "shut_up_timestamp", "muted_until", "mutedUntil"));
      jdbcTemplate.update(
          """
          INSERT INTO bot_group_member
            (group_id, user_id, nickname, card, role, join_time, last_sent_time, muted_until)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), card = VALUES(card), role = VALUES(role),
                                  join_time = VALUES(join_time), last_sent_time = VALUES(last_sent_time),
                                  muted_until = VALUES(muted_until), updated_at = CURRENT_TIMESTAMP
          """,
          groupId,
          userId,
          text(firstPresent(item, "nickname")),
          text(firstPresent(item, "card")),
          role,
          joinTime,
          lastSentTime,
          mutedUntil);
    }
    jdbcTemplate.update(
        """
        UPDATE bot_group
        SET member_count = ?, admin_count = ?, owner_id = COALESCE(?, owner_id),
            owner_nickname = COALESCE(?, owner_nickname), last_synced_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE group_id = ?
        """,
        members.size(),
        adminCount,
        ownerId,
        ownerNickname,
        groupId);
    unifiedGroupProjectionService.syncBotGroup(groupId);
    logOperation("群成员", "同步群成员", groupId, "同步群 " + groupId + " 成员：" + members.size() + " 人");
    return Result.success(Map.of("syncedCount", members.size()), "群成员已同步");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateGroupConfig(String groupId, Map<String, Object> request) {
    String error = applyGroupConfig(groupId, request);
    if (error != null) {
      return Result.error(400, error);
    }
    unifiedGroupProjectionService.syncBotGroup(groupId);
    logOperation("群配置", "更新群配置", groupId, "更新群 " + groupId + " 机器人和插件配置");
    return Result.success("群配置已保存");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> batchUpdateGroupConfig(Map<String, Object> request) {
    List<String> groupIds = asStringList(request.get("groupIds"));
    if (groupIds.isEmpty()) {
      return Result.error(400, "请选择要批量操作的群");
    }
    int count = 0;
    for (String groupId : groupIds) {
      String error = applyGroupConfig(groupId, request);
      if (error != null) {
        return Result.error(400, "群 " + groupId + " 更新失败：" + error);
      }
      unifiedGroupProjectionService.syncBotGroup(groupId);
      count++;
    }
    logOperation("批量群配置", "批量更新群配置", "batch", "批量更新 QQ 群配置：" + count + " 个");
    return Result.success("已批量更新 " + count + " 个群");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> muteMember(String groupId, String userId, Map<String, Object> request) {
    if (findGroup(groupId) == null) {
      return Result.error(404, "群聊不存在");
    }
    int duration = Math.max(0, toInteger(request.get("duration"), 0));
    NapCatResponse response = napCatClient.muteMember(groupId, userId, duration);
    if (!response.ok()) {
      logOperation("成员禁言", "禁言失败", groupId + ":" + userId, response.message());
      return Result.error(502, "禁言操作失败：" + response.message());
    }
    Timestamp mutedUntil = duration > 0 ? Timestamp.from(Instant.now().plusSeconds(duration)) : null;
    jdbcTemplate.update(
        "UPDATE bot_group_member SET muted_until = ?, updated_at = CURRENT_TIMESTAMP WHERE group_id = ? AND user_id = ?",
        mutedUntil,
        groupId,
        userId);
    logOperation(
        "成员禁言",
        duration > 0 ? "禁言成员" : "解除禁言",
        groupId + ":" + userId,
        duration > 0 ? "禁言 " + duration + " 秒" : "解除禁言");
    return Result.success(duration > 0 ? "成员已禁言" : "成员已解除禁言");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> muteAll(String groupId, Map<String, Object> request) {
    if (findGroup(groupId) == null) {
      return Result.error(404, "群聊不存在");
    }
    boolean enabled = toBoolean(request.get("enabled"), false);
    NapCatResponse response = napCatClient.muteAll(groupId, enabled);
    if (!response.ok()) {
      logOperation("全体禁言", "全体禁言失败", groupId, response.message());
      return Result.error(502, "全体禁言操作失败：" + response.message());
    }
    logOperation("全体禁言", enabled ? "开启全体禁言" : "关闭全体禁言", groupId, "群 " + groupId);
    return Result.success(enabled ? "已开启全体禁言" : "已关闭全体禁言");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> sendGroupMessage(String groupId, Map<String, Object> request) {
    if (findGroup(groupId) == null) {
      return Result.error(404, "群聊不存在");
    }
    String content = trimToNull(request.get("content"));
    if (content == null) {
      return Result.error(400, "请填写群消息内容");
    }
    boolean atAll = toBoolean(request.get("atAll"), false);
    String deliveryMode = trimToNull(firstPresent(request, "deliveryMode", "sendMode", "mode"));
    Timestamp scheduledAt = timestampFromValue(firstPresent(request, "scheduledAt", "scheduled_at"));
    if ("scheduled".equals(deliveryMode) || scheduledAt != null) {
      if (scheduledAt == null) {
        return Result.error(400, "请选择定时发送时间");
      }
      if (!scheduledAt.after(Timestamp.from(Instant.now()))) {
        return Result.error(400, "定时发送时间必须晚于当前时间");
      }
      Integer id = insertGroupMessageTask(groupId, content, atAll, "pending", scheduledAt, null, "等待定时发送");
      logOperation("群消息", "创建定时消息", groupId + ":" + id, messageLogContent(content, atAll));
      Map<String, Object> result = groupMessageResult(id, "pending", "等待定时发送", null);
      result.put("scheduledAt", scheduledAt);
      return Result.success(result, "定时群消息已创建");
    }

    NapCatResponse response = napCatClient.sendGroupMessage(groupId, formatGroupMessage(content, atAll));
    String status = response.ok() ? "sent" : "failed";
    Integer id =
        insertGroupMessageTask(
            groupId,
            content,
            atAll,
            status,
            null,
            response.ok() ? Timestamp.from(Instant.now()) : null,
            response.message());
    logOperation("群消息", response.ok() ? "发送群消息" : "发送群消息失败", groupId + ":" + id, response.message());
    return Result.success(
        groupMessageResult(id, status, response.message(), null),
        response.ok() ? "群消息已发送" : "群消息发送失败，已记录失败原因");
  }

  @Override
  public Result<List<Map<String, Object>>> groupMessages(String groupId) {
    return Result.success(
        queryForList(
            """
            SELECT id, group_id AS groupId, content, at_all AS atAll, status, scheduled_at AS scheduledAt,
                   sent_at AS sentAt, result_message AS resultMessage, created_by AS createdBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM bot_group_message_task
            WHERE group_id = ?
            ORDER BY COALESCE(scheduled_at, created_at) DESC, id DESC
            LIMIT 80
            """,
            groupId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> sendScheduledGroupMessageNow(String groupId, Integer messageId) {
    Map<String, Object> message =
        queryForOptionalMap(
            "SELECT * FROM bot_group_message_task WHERE group_id = ? AND id = ?", groupId, messageId);
    if (message == null) {
      return Result.error(404, "群消息任务不存在");
    }
    String status = trimToNull(message.get("status"));
    if (!"pending".equals(status)) {
      return Result.error(400, "只有待发送的定时消息可以立即执行");
    }
    int locked =
        jdbcTemplate.update(
            """
            UPDATE bot_group_message_task
            SET status = 'sending', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'pending'
            """,
            messageId);
    if (locked == 0) {
      return Result.error(409, "这条定时消息正在被处理，请刷新后重试");
    }
    return Result.success(dispatchGroupMessageTask(message), "定时群消息已立即执行");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteGroupMessage(String groupId, Integer messageId) {
    Map<String, Object> message =
        queryForOptionalMap(
            "SELECT status FROM bot_group_message_task WHERE group_id = ? AND id = ?", groupId, messageId);
    if (message == null) {
      return Result.error(404, "群消息任务不存在");
    }
    String status = trimToNull(message.get("status"));
    if ("pending".equals(status)) {
      jdbcTemplate.update(
          """
          UPDATE bot_group_message_task
          SET status = 'canceled', result_message = '已取消', updated_at = CURRENT_TIMESTAMP
          WHERE group_id = ? AND id = ? AND status = 'pending'
          """,
          groupId,
          messageId);
      logOperation("群消息", "取消定时消息", groupId + ":" + messageId, "取消定时群消息");
      return Result.success("定时群消息已取消");
    }
    int rows =
        jdbcTemplate.update(
            "DELETE FROM bot_group_message_task WHERE group_id = ? AND id = ?", groupId, messageId);
    if (rows == 0) {
      return Result.error(404, "群消息任务不存在");
    }
    logOperation("群消息", "删除消息记录", groupId + ":" + messageId, "删除群消息记录");
    return Result.success("群消息记录已删除");
  }

  @Scheduled(
      initialDelayString = "${app.bot.group-message-task-initial-delay-ms:10000}",
      fixedDelayString = "${app.bot.group-message-task-scan-ms:30000}")
  public void processDueGroupMessages() {
    List<Map<String, Object>> rows =
        queryForList(
            """
            SELECT *
            FROM bot_group_message_task
            WHERE status = 'pending' AND scheduled_at <= CURRENT_TIMESTAMP
            ORDER BY scheduled_at ASC, id ASC
            LIMIT 10
            """);
    for (Map<String, Object> row : rows) {
      Integer id = toInteger(row.get("id"), null);
      if (id == null) {
        continue;
      }
      int locked =
          jdbcTemplate.update(
              """
              UPDATE bot_group_message_task
              SET status = 'sending', updated_at = CURRENT_TIMESTAMP
              WHERE id = ? AND status = 'pending'
              """,
              id);
      if (locked == 0) {
        continue;
      }
      try {
        dispatchGroupMessageTask(row);
      } catch (RuntimeException ex) {
        log.warn("Failed to dispatch scheduled group message, id={}", id, ex);
        jdbcTemplate.update(
            """
            UPDATE bot_group_message_task
            SET status = 'failed', result_message = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            firstNonBlank(ex.getMessage(), "定时消息发送异常"),
            id);
      }
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> publishAnnouncement(String groupId, Map<String, Object> request) {
    if (findGroup(groupId) == null) {
      return Result.error(404, "群聊不存在");
    }
    String title = trimToNull(request.get("title"));
    String content = trimToNull(request.get("content"));
    if (title == null || content == null) {
      return Result.error(400, "请填写公告标题和正文");
    }
    String attachments = jsonOf(request.get("attachments"));
    String announcementText = formatAnnouncement(title, content);
    String lengthError = announcementLengthError(announcementText);
    if (lengthError != null) {
      logOperation("群公告", "发布公告失败", groupId, title + " / " + lengthError);
      return Result.error(400, lengthError);
    }
    NapCatResponse response = napCatClient.sendGroupNotice(groupId, announcementText);
    String status = response.ok() ? "published" : "failed";
    String noticeId = response.ok() ? noticeIdFrom(response) : null;
    Integer id = insertAnnouncement(groupId, title, content, attachments, status, noticeId, response.message());
    logOperation("群公告", "发布公告", groupId + ":" + id, title + " / " + status);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("status", status);
    result.put("noticeId", noticeId);
    result.put("message", response.message());
    return Result.success(result, response.ok() ? "群公告已发布" : "群公告发布失败，已记录失败原因");
  }

  @Override
  public Result<List<Map<String, Object>>> announcements(String groupId) {
    return Result.success(
        queryForList(
            """
            SELECT id, group_id AS groupId, title, content, attachments, status, result_message AS resultMessage,
                   notice_id AS noticeId, published_by AS publishedBy, published_at AS publishedAt,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM bot_group_announcement
            WHERE group_id = ?
            ORDER BY created_at DESC, id DESC
            """,
            groupId));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteAnnouncement(String groupId, Integer announcementId) {
    Map<String, Object> announcement =
        queryForOptionalMap(
            "SELECT * FROM bot_group_announcement WHERE group_id = ? AND id = ?", groupId, announcementId);
    if (announcement == null) {
      return Result.error(404, "公告不存在");
    }
    String noticeId = trimToNull(announcement.get("notice_id"));
    if (noticeId == null) {
      noticeId = findAnnouncementNoticeId(groupId, announcement);
    }
    if (noticeId != null) {
      NapCatResponse response = napCatClient.deleteGroupNotice(groupId, noticeId);
      if (!response.ok()) {
        return Result.error(502, "删除 QQ 群公告失败：" + response.message());
      }
    }
    int rows =
        jdbcTemplate.update(
            "DELETE FROM bot_group_announcement WHERE group_id = ? AND id = ?", groupId, announcementId);
    if (rows == 0) {
      return Result.error(404, "公告不存在");
    }
    logOperation("群公告", "删除公告", groupId + ":" + announcementId, "删除群公告");
    return Result.success("公告已删除");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> republishAnnouncement(String groupId, Integer announcementId) {
    Map<String, Object> announcement =
        queryForOptionalMap(
            "SELECT * FROM bot_group_announcement WHERE group_id = ? AND id = ?", groupId, announcementId);
    if (announcement == null) {
      return Result.error(404, "公告不存在");
    }
    String title = text(announcement.get("title"));
    String content = text(announcement.get("content"));
    String announcementText = formatAnnouncement(title, content);
    String lengthError = announcementLengthError(announcementText);
    if (lengthError != null) {
      jdbcTemplate.update(
          """
          UPDATE bot_group_announcement
          SET status = 'failed', result_message = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
          """,
          lengthError,
          announcementId);
      logOperation("群公告", "重新发布公告失败", groupId + ":" + announcementId, title + " / " + lengthError);
      return Result.error(400, lengthError);
    }
    NapCatResponse response = napCatClient.sendGroupNotice(groupId, announcementText);
    String status = response.ok() ? "published" : "failed";
    String noticeId = response.ok() ? noticeIdFrom(response) : null;
    jdbcTemplate.update(
        """
        UPDATE bot_group_announcement
        SET status = ?, notice_id = COALESCE(?, notice_id), result_message = ?, published_by = ?,
            published_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        status,
        noticeId,
        response.message(),
        currentUserId(),
        announcementId);
    logOperation("群公告", "重新发布公告", groupId + ":" + announcementId, title + " / " + status);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", announcementId);
    result.put("status", status);
    result.put("noticeId", noticeId);
    result.put("message", response.message());
    return Result.success(
        result,
        response.ok() ? "群公告已重新发布" : "群公告重新发布失败，已记录失败原因");
  }

  @Override
  public Result<List<Map<String, Object>>> joinRequests(String groupId, String status) {
    syncJoinRequestsFromSystemMessages(groupId);
    List<Object> args = new ArrayList<>();
    args.add(groupId);
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT id, group_id AS groupId, request_id AS requestId, flag, user_id AS userId, nickname,
                   comment, status, reason, handled_by AS handledBy, requested_at AS requestedAt,
                   handled_at AS handledAt, created_at AS createdAt, updated_at AS updatedAt
            FROM bot_join_request
            WHERE group_id = ?
            """);
    if (status != null && !status.isBlank()) {
      sql.append(" AND status = ?");
      args.add(status.trim());
    }
    sql.append(" ORDER BY COALESCE(requested_at, created_at) DESC, id DESC");
    return Result.success(queryForList(sql.toString(), args.toArray()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<Map<String, Object>> saveJoinRequest(String groupId, Map<String, Object> request) {
    String requestId = trimToNull(request.get("requestId"));
    String flag = trimToNull(request.get("flag"));
    String userId = idString(request.get("userId"));
    if (requestId == null && flag == null) {
      requestId = groupId + "-" + userId + "-" + System.currentTimeMillis();
    }
    jdbcTemplate.update(
        """
        INSERT INTO bot_join_request
          (group_id, request_id, flag, user_id, nickname, comment, status, requested_at)
        VALUES (?, ?, ?, ?, ?, ?, 'pending', COALESCE(?, CURRENT_TIMESTAMP))
        ON DUPLICATE KEY UPDATE flag = COALESCE(VALUES(flag), flag),
                                user_id = COALESCE(VALUES(user_id), user_id),
                                nickname = COALESCE(VALUES(nickname), nickname),
                                comment = COALESCE(VALUES(comment), comment),
                                status = 'pending', updated_at = CURRENT_TIMESTAMP
        """,
        groupId,
        requestId,
        flag,
        userId,
        trimToNull(request.get("nickname")),
        trimToNull(request.get("comment")),
        timestampFromValue(request.get("requestedAt")));
    return Result.success(Map.of("requestId", requestId == null ? "" : requestId), "入群申请已记录");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> handleJoinRequest(String groupId, Integer requestId, Map<String, Object> request) {
    Map<String, Object> row =
        queryForOptionalMap("SELECT * FROM bot_join_request WHERE group_id = ? AND id = ?", groupId, requestId);
    if (row == null) {
      return Result.error(404, "入群申请不存在");
    }
    boolean approve = toBoolean(request.get("approve"), false);
    String reason = trimToNull(request.get("reason"));
    String flag = firstNonBlank(trimToNull(row.get("flag")), trimToNull(row.get("request_id")));
    if (flag == null) {
      return Result.error(400, "这条入群申请缺少 NapCat 处理标识，无法在后台处理；请等待新的事件推送或在 QQ/NapCat 里处理。");
    }
    NapCatResponse response = napCatClient.handleGroupRequest(flag, approve, reason);
    if (!response.ok()) {
      logOperation("入群申请", "处理失败", groupId + ":" + requestId, response.message());
      return Result.error(502, "处理入群申请失败：" + response.message());
    }
    jdbcTemplate.update(
        """
        UPDATE bot_join_request
        SET status = ?, reason = ?, handled_by = ?, handled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        approve ? "approved" : "rejected",
        reason,
        currentUserId(),
        requestId);
    logOperation("入群申请", approve ? "同意入群" : "拒绝入群", groupId + ":" + requestId, reason);
    return Result.success(approve ? "已同意入群申请" : "已拒绝入群申请");
  }

  @Override
  public Result<List<Map<String, Object>>> sensitiveWords() {
    return Result.success(
        queryForList(
            """
            SELECT id, word, action, enabled, created_by AS createdBy, created_at AS createdAt, updated_at AS updatedAt
            FROM bot_sensitive_word
            ORDER BY enabled DESC, id DESC
            """));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> saveSensitiveWord(Map<String, Object> request) {
    String word = trimToNull(request.get("word"));
    if (word == null) {
      return Result.error(400, "请填写敏感词");
    }
    jdbcTemplate.update(
        """
        INSERT INTO bot_sensitive_word (word, action, enabled, created_by)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE action = VALUES(action), enabled = VALUES(enabled), updated_at = CURRENT_TIMESTAMP
        """,
        word,
        trimToNull(request.getOrDefault("action", "warn")),
        toBoolean(request.get("enabled"), true),
        currentUserId());
    logOperation("敏感词", "保存敏感词", word, "保存敏感词：" + word);
    return Result.success("敏感词已保存");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateSensitiveWord(Integer wordId, Map<String, Object> request) {
    String word = trimToNull(request.get("word"));
    if (word == null) {
      return Result.error(400, "请填写敏感词");
    }
    int rows =
        jdbcTemplate.update(
            "UPDATE bot_sensitive_word SET word = ?, action = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            word,
            trimToNull(request.getOrDefault("action", "warn")),
            toBoolean(request.get("enabled"), true),
            wordId);
    if (rows == 0) {
      return Result.error(404, "敏感词不存在");
    }
    return Result.success("敏感词已更新");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteSensitiveWord(Integer wordId) {
    int rows = jdbcTemplate.update("DELETE FROM bot_sensitive_word WHERE id = ?", wordId);
    return rows > 0 ? Result.success("敏感词已删除") : Result.error(404, "敏感词不存在");
  }

  @Override
  public Result<List<Map<String, Object>>> autoReviewRules(String groupId) {
    List<Object> args = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT id, name, group_id AS groupId, keywords, approve, enabled, created_by AS createdBy,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM bot_auto_review_rule
            WHERE 1 = 1
            """);
    if (groupId != null && !groupId.isBlank()) {
      sql.append(" AND (group_id = ? OR group_id IS NULL)");
      args.add(groupId);
    }
    sql.append(" ORDER BY enabled DESC, id DESC");
    return Result.success(queryForList(sql.toString(), args.toArray()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> saveAutoReviewRule(Map<String, Object> request) {
    String name = trimToNull(request.get("name"));
    if (name == null) {
      return Result.error(400, "请填写规则名称");
    }
    jdbcTemplate.update(
        """
        INSERT INTO bot_auto_review_rule (name, group_id, keywords, approve, enabled, created_by)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        name,
        trimToNull(request.get("groupId")),
        jsonOf(request.get("keywords")),
        toBoolean(request.get("approve"), true),
        toBoolean(request.get("enabled"), true),
        currentUserId());
    return Result.success("自动审核规则已创建");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateAutoReviewRule(Integer ruleId, Map<String, Object> request) {
    String name = trimToNull(request.get("name"));
    if (name == null) {
      return Result.error(400, "请填写规则名称");
    }
    int rows =
        jdbcTemplate.update(
            """
            UPDATE bot_auto_review_rule
            SET name = ?, group_id = ?, keywords = ?, approve = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            name,
            trimToNull(request.get("groupId")),
            jsonOf(request.get("keywords")),
            toBoolean(request.get("approve"), true),
            toBoolean(request.get("enabled"), true),
            ruleId);
    return rows > 0 ? Result.success("自动审核规则已更新") : Result.error(404, "自动审核规则不存在");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteAutoReviewRule(Integer ruleId) {
    int rows = jdbcTemplate.update("DELETE FROM bot_auto_review_rule WHERE id = ?", ruleId);
    return rows > 0 ? Result.success("自动审核规则已删除") : Result.error(404, "自动审核规则不存在");
  }

  @Override
  public Result<List<Map<String, Object>>> statistics(String groupId, String startDate, String endDate) {
    LocalDate end = parseDate(endDate, LocalDate.now(ZONE));
    LocalDate start = parseDate(startDate, end.minusDays(13));
    String normalizedGroupId = idString(groupId);
    if (normalizedGroupId != null) {
      return Result.success(statisticsByGroup(normalizedGroupId, start, end));
    }
    return Result.success(
        queryForList(
            """
            SELECT stat_date AS statDate, SUM(message_count) AS messageCount,
                   SUM(active_member_count) AS activeMemberCount, SUM(command_count) AS commandCount
            FROM bot_message_stat
            WHERE stat_date BETWEEN ? AND ?
            GROUP BY stat_date
            ORDER BY stat_date ASC
            """,
            Date.valueOf(start),
            Date.valueOf(end)));
  }

  @Override
  public Result<List<Map<String, Object>>> activeGroups(String startDate, String endDate) {
    LocalDate end = parseDate(endDate, LocalDate.now(ZONE));
    LocalDate start = parseDate(startDate, end.minusDays(13));
    return Result.success(
        queryForList(
            """
            SELECT g.group_id AS groupId, COALESCE(g.group_name, s.group_id) AS groupName,
                   SUM(s.message_count) AS messageCount, SUM(s.command_count) AS commandCount,
                   MAX(s.active_member_count) AS peakActiveMemberCount
            FROM bot_message_stat s
            LEFT JOIN bot_group g ON g.group_id = s.group_id
            WHERE s.stat_date BETWEEN ? AND ?
            GROUP BY s.group_id, g.group_name, g.group_id
            ORDER BY messageCount DESC
            """,
            Date.valueOf(start),
            Date.valueOf(end)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> recordMessageStat(Map<String, Object> request) {
    String groupId = idString(request.get("groupId"));
    if (groupId == null) {
      return Result.error(400, "缺少群号");
    }
    String userId = idString(firstPresent(request, "userId", "user_id", "senderId", "sender_id"));
    LocalDate statDate = parseDate(text(request.get("statDate")), LocalDate.now(ZONE));
    int messageCount = Math.max(0, toInteger(request.get("messageCount"), 1));
    int activeMemberCount = Math.max(0, toInteger(request.get("activeMemberCount"), 0));
    int commandCount = Math.max(0, toInteger(request.get("commandCount"), 0));
    jdbcTemplate.update(
        """
        INSERT INTO bot_message_stat (group_id, stat_date, message_count, active_member_count, command_count)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE message_count = message_count + VALUES(message_count),
                                active_member_count = GREATEST(active_member_count, VALUES(active_member_count)),
                                command_count = command_count + VALUES(command_count),
                                updated_at = CURRENT_TIMESTAMP
        """,
        groupId,
        Date.valueOf(statDate),
        messageCount,
        activeMemberCount,
        commandCount);
    if (userId != null) {
      jdbcTemplate.update(
          """
          INSERT IGNORE INTO bot_message_active_member (group_id, stat_date, user_id)
          VALUES (?, ?, ?)
          """,
          groupId,
          Date.valueOf(statDate),
          userId);
      jdbcTemplate.update(
          """
          UPDATE bot_message_stat
          SET active_member_count = (
              SELECT COUNT(*)
              FROM bot_message_active_member
              WHERE group_id = ? AND stat_date = ?
          ), updated_at = CURRENT_TIMESTAMP
          WHERE group_id = ? AND stat_date = ?
          """,
          groupId,
          Date.valueOf(statDate),
          groupId,
          Date.valueOf(statDate));
      jdbcTemplate.update(
          """
          UPDATE bot_group_member
          SET last_sent_time = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
          WHERE group_id = ? AND user_id = ?
          """,
          groupId,
          userId);
    }
    jdbcTemplate.update(
        "UPDATE bot_group SET last_active_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE group_id = ?",
        groupId);
    return Result.success("消息统计已记录");
  }

  private String applyGroupConfig(String groupId, Map<String, Object> request) {
    if (findGroup(groupId) == null) {
      return "群聊不存在";
    }
    String mode = trimToNull(request.get("mode"));
    if (mode != null && !BOT_MODES.contains(mode)) {
      return "机器人响应模式不合法";
    }
    Boolean botEnabled =
        request.containsKey("botEnabled") ? toBoolean(request.get("botEnabled"), true) : null;
    if (botEnabled != null || mode != null) {
      jdbcTemplate.update(
          """
          UPDATE bot_group
          SET bot_enabled = COALESCE(?, bot_enabled), mode = COALESCE(?, mode), updated_at = CURRENT_TIMESTAMP
          WHERE group_id = ?
          """,
          botEnabled,
          mode,
          groupId);
    }
    ensureGroupConfig(groupId);
    jdbcTemplate.update(
        """
        UPDATE bot_group_config
        SET welcome_enabled = ?, welcome_text = ?, at_new_member = ?, welcome_image_url = ?,
            welcome_attachment_url = ?, welcome_delay_seconds = ?, plugin_config = ?,
            auto_review_enabled = ?, sensitive_filter_enabled = ?, updated_at = CURRENT_TIMESTAMP
        WHERE group_id = ?
        """,
        toBoolean(request.get("welcomeEnabled"), false),
        trimToNull(request.get("welcomeText")),
        toBoolean(request.get("atNewMember"), true),
        trimToNull(request.get("welcomeImageUrl")),
        trimToNull(request.get("welcomeAttachmentUrl")),
        Math.max(0, toInteger(request.get("welcomeDelaySeconds"), 0)),
        jsonOf(request.getOrDefault("pluginConfig", Map.of())),
        toBoolean(request.get("autoReviewEnabled"), false),
        toBoolean(request.get("sensitiveFilterEnabled"), false),
        groupId);
    return null;
  }

  private List<Map<String, Object>> statisticsByGroup(String groupId, LocalDate start, LocalDate end) {
    return queryForList(
        """
        SELECT stat_date AS statDate, message_count AS messageCount,
               active_member_count AS activeMemberCount, command_count AS commandCount
        FROM bot_message_stat
        WHERE group_id = ? AND stat_date BETWEEN ? AND ?
        ORDER BY stat_date ASC
        """,
        groupId,
        Date.valueOf(start),
        Date.valueOf(end));
  }

  private Map<String, Object> findGroup(String groupId) {
    return queryForOptionalMap(
        """
        SELECT id, bot_account_id AS botAccountId, group_id AS groupId, group_name AS groupName,
               owner_id AS ownerId, owner_nickname AS ownerNickname, member_count AS memberCount,
               admin_count AS adminCount, bot_role AS botRole, bot_enabled AS botEnabled, mode,
               last_active_at AS lastActiveAt, last_synced_at AS lastSyncedAt, created_at AS createdAt,
               updated_at AS updatedAt
        FROM bot_group
        WHERE group_id = ?
        """,
        groupId);
  }

  private Map<String, Object> findConfig(String groupId) {
    return queryForOptionalMap(
        """
        SELECT id, group_id AS groupId, welcome_enabled AS welcomeEnabled, welcome_text AS welcomeText,
               at_new_member AS atNewMember, welcome_image_url AS welcomeImageUrl,
               welcome_attachment_url AS welcomeAttachmentUrl, welcome_delay_seconds AS welcomeDelaySeconds,
               plugin_config AS pluginConfig, auto_review_enabled AS autoReviewEnabled,
               auto_review_keywords AS autoReviewKeywords, sensitive_filter_enabled AS sensitiveFilterEnabled,
               created_at AS createdAt, updated_at AS updatedAt
        FROM bot_group_config
        WHERE group_id = ?
        """,
        groupId);
  }

  private void ensureGroupConfig(String groupId) {
    jdbcTemplate.update(
        """
        INSERT INTO bot_group_config (group_id, welcome_enabled, welcome_text, at_new_member, plugin_config)
        SELECT ?, 0, '欢迎 {nickname} 加入本群！', 1, '{}'
        WHERE NOT EXISTS (SELECT 1 FROM bot_group_config WHERE group_id = ?)
        """,
        groupId,
        groupId);
  }

  private void ensureMinimalGroup(String groupId) {
    jdbcTemplate.update(
        """
        INSERT INTO bot_group (bot_account_id, group_id, group_name, last_active_at)
        VALUES (1, ?, ?, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE last_active_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        """,
        groupId,
        "QQ群 " + groupId);
    ensureGroupConfig(groupId);
  }

  private void upsertJoinedMember(String groupId, String userId) {
    if (userId == null) {
      return;
    }
    jdbcTemplate.update(
        """
        INSERT INTO bot_group_member (group_id, user_id, nickname, role, join_time)
        VALUES (?, ?, ?, 'member', CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE join_time = COALESCE(join_time, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP
        """,
        groupId,
        userId,
        userId);
  }

  private void sendWelcomeIfEnabled(String groupId, String userId, String nickname) {
    if (userId == null) {
      return;
    }
    Map<String, Object> config = findConfig(groupId);
    if (config == null || !toBoolean(config.get("welcomeEnabled"), false)) {
      return;
    }
    Map<String, Object> pluginConfig = parseJsonMap(config.get("pluginConfig"));
    if (pluginConfig.containsKey("welcome") && !toBoolean(pluginConfig.get("welcome"), true)) {
      return;
    }
    int delaySeconds = Math.max(0, toInteger(config.get("welcomeDelaySeconds"), 0));
    Runnable task = () -> sendWelcomeMessage(groupId, userId, nickname, config);
    if (delaySeconds > 0) {
      CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS).execute(task);
    } else {
      task.run();
    }
  }

  private void sendWelcomeMessage(String groupId, String userId, String nickname, Map<String, Object> config) {
    try {
      Map<String, Object> group = findGroup(groupId);
      String groupName = group == null ? "QQ群 " + groupId : text(group.get("groupName"));
      Map<String, Object> memberInfo = resolveMemberInfo(groupId, userId);
      String displayName =
          firstNonBlank(nickname, text(memberInfo.get("card")), text(memberInfo.get("nickname")), userId);
      if (!memberInfo.isEmpty()) {
        updateMemberProfile(groupId, userId, text(memberInfo.get("nickname")), text(memberInfo.get("card")));
      }
      String template =
          trimToNull(config.get("welcomeText")) == null
              ? "欢迎 {nickname} 加入本群！"
              : trimToNull(config.get("welcomeText"));
      String message =
          template
              .replace("{nickname}", displayName)
              .replace("{qq}", userId)
              .replace("{group_name}", groupName == null ? groupId : groupName)
              .replace("{group_id}", groupId);
      String imageUrl = trimToNull(config.get("welcomeImageUrl"));
      if (imageUrl != null) {
        message = message + "\n[CQ:image,file=" + escapeCqValue(imageUrl) + "]";
      }
      String attachmentUrl = trimToNull(config.get("welcomeAttachmentUrl"));
      if (attachmentUrl != null) {
        message = message + "\n" + attachmentUrl;
      }
      if (toBoolean(config.get("atNewMember"), true)) {
        message = "[CQ:at,qq=" + userId + "] " + message;
      }
      NapCatResponse response = napCatClient.sendGroupMessage(groupId, message);
      logOperation("新人欢迎", response.ok() ? "发送欢迎语" : "欢迎语发送失败", groupId + ":" + userId, response.message());
    } catch (RuntimeException ex) {
      log.warn("Failed to send welcome message, groupId={}, userId={}", groupId, userId, ex);
    }
  }

  private Map<String, Object> resolveMemberInfo(String groupId, String userId) {
    NapCatResponse response = napCatClient.getGroupMemberInfo(groupId, userId);
    if (!response.ok() || !(response.data() instanceof Map<?, ?> map)) {
      return Map.of();
    }
    return toStringKeyMap(map);
  }

  private void updateMemberProfile(String groupId, String userId, String nickname, String card) {
    jdbcTemplate.update(
        """
        UPDATE bot_group_member
        SET nickname = COALESCE(?, nickname), card = COALESCE(?, card), updated_at = CURRENT_TIMESTAMP
        WHERE group_id = ? AND user_id = ?
        """,
        trimToNull(nickname),
        trimToNull(card),
        groupId,
        userId);
  }

  private void syncJoinRequestsFromSystemMessages(String groupId) {
    NapCatResponse response = napCatClient.getGroupSystemMessages();
    if (!response.ok() || !(response.data() instanceof Map<?, ?> map)) {
      return;
    }
    Map<String, Object> data = toStringKeyMap(map);
    for (Map<String, Object> item : asMapList(data.get("join_requests"))) {
      String requestGroupId = idString(firstPresent(item, "group_id", "groupId"));
      if (!groupId.equals(requestGroupId) || toBoolean(item.get("checked"), false)) {
        continue;
      }
      Map<String, Object> joinRequest = new LinkedHashMap<>();
      joinRequest.put("requestId", idString(firstPresent(item, "request_id", "requestId")));
      joinRequest.put("flag", trimToNull(item.get("flag")));
      joinRequest.put("userId", idString(firstPresent(item, "requester_uin", "requesterUin", "user_id", "userId")));
      joinRequest.put("nickname", trimToNull(firstPresent(item, "requester_nick", "requesterNick", "nickname")));
      joinRequest.put("comment", trimToNull(firstPresent(item, "message", "comment")));
      saveJoinRequest(groupId, joinRequest);
    }
  }

  private void applyAutoReviewIfMatched(String groupId, Map<String, Object> joinRequest) {
    Map<String, Object> config = findConfig(groupId);
    if (config == null || !toBoolean(config.get("autoReviewEnabled"), false)) {
      return;
    }
    String flag = trimToNull(joinRequest.get("flag"));
    if (flag == null) {
      return;
    }
    String comment = trimToNull(joinRequest.get("comment"));
    List<Map<String, Object>> rules =
        queryForList(
            """
            SELECT id, name, keywords, approve
            FROM bot_auto_review_rule
            WHERE enabled = 1 AND (group_id = ? OR group_id IS NULL)
            ORDER BY group_id DESC, id ASC
            """,
            groupId);
    for (Map<String, Object> rule : rules) {
      if (!keywordsMatch(rule.get("keywords"), comment)) {
        continue;
      }
      boolean approve = toBoolean(rule.get("approve"), true);
      NapCatResponse response =
          napCatClient.handleGroupRequest(flag, approve, approve ? null : "未通过自动审核规则");
      Integer id = findJoinRequestId(groupId, flag);
      if (id != null && response.ok()) {
        jdbcTemplate.update(
            """
            UPDATE bot_join_request
            SET status = ?, reason = ?, handled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            approve ? "approved" : "rejected",
            "自动审核：" + text(rule.get("name")),
            id);
      }
      logOperation("自动审核", approve ? "自动同意" : "自动拒绝", groupId + ":" + flag, response.message());
      return;
    }
  }

  private Integer findJoinRequestId(String groupId, String flag) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT id FROM bot_join_request WHERE group_id = ? AND flag = ? ORDER BY id DESC LIMIT 1",
          Integer.class,
          groupId,
          flag);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  private Integer insertGroupMessageTask(
      String groupId,
      String content,
      boolean atAll,
      String status,
      Timestamp scheduledAt,
      Timestamp sentAt,
      String resultMessage) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  """
                  INSERT INTO bot_group_message_task
                    (group_id, content, at_all, status, scheduled_at, sent_at, result_message, created_by)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, groupId);
          ps.setString(2, content);
          ps.setBoolean(3, atAll);
          ps.setString(4, status);
          ps.setTimestamp(5, scheduledAt);
          ps.setTimestamp(6, sentAt);
          ps.setString(7, resultMessage);
          Integer userId = currentUserId();
          if (userId == null) {
            ps.setObject(8, null);
          } else {
            ps.setInt(8, userId);
          }
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    return key == null ? null : key.intValue();
  }

  private Map<String, Object> dispatchGroupMessageTask(Map<String, Object> row) {
    Integer id = toInteger(row.get("id"), null);
    String groupId = idString(firstPresent(row, "group_id", "groupId"));
    String content = text(row.get("content"));
    boolean atAll = toBoolean(firstPresent(row, "at_all", "atAll"), false);
    NapCatResponse response = napCatClient.sendGroupMessage(groupId, formatGroupMessage(content, atAll));
    String status = response.ok() ? "sent" : "failed";
    jdbcTemplate.update(
        """
        UPDATE bot_group_message_task
        SET status = ?, result_message = ?, sent_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE sent_at END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        status,
        response.message(),
        response.ok(),
        id);
    logOperation("群消息", response.ok() ? "发送定时消息" : "定时消息发送失败", groupId + ":" + id, response.message());
    return groupMessageResult(id, status, response.message(), null);
  }

  private Map<String, Object> groupMessageResult(Integer id, String status, String message, Object extra) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("status", status);
    result.put("message", message);
    if (extra != null) {
      result.put("data", extra);
    }
    return result;
  }

  private String formatGroupMessage(String content, boolean atAll) {
    String message = text(content);
    if (!atAll) {
      return message;
    }
    return "[CQ:at,qq=all]\n" + message;
  }

  private String messageLogContent(String content, boolean atAll) {
    String prefix = atAll ? "@全体 " : "";
    String text = content == null ? "" : content.strip();
    if (text.length() > 120) {
      text = text.substring(0, 120) + "...";
    }
    return prefix + text;
  }

  private Integer insertAnnouncement(
      String groupId,
      String title,
      String content,
      String attachments,
      String status,
      String noticeId,
      String message) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  """
                  INSERT INTO bot_group_announcement
                    (group_id, title, content, attachments, status, notice_id, result_message, published_by, published_at)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, groupId);
          ps.setString(2, title);
          ps.setString(3, content);
          ps.setString(4, attachments);
          ps.setString(5, status);
          ps.setString(6, noticeId);
          ps.setString(7, message);
          Integer userId = currentUserId();
          if (userId == null) {
            ps.setObject(8, null);
          } else {
            ps.setInt(8, userId);
          }
          return ps;
        },
        keyHolder);
    Number key = keyHolder.getKey();
    return key == null ? null : key.intValue();
  }

  private String formatAnnouncement(String title, String content) {
    return title + "\n\n" + content;
  }

  private String announcementLengthError(String announcementText) {
    int length = codePointLength(announcementText);
    if (length <= GROUP_NOTICE_MAX_TEXT_LENGTH) {
      return null;
    }
    return "群公告内容过长：当前 "
        + length
        + " 字，最多支持 "
        + GROUP_NOTICE_MAX_TEXT_LENGTH
        + " 字，请精简后再发布";
  }

  private int codePointLength(String value) {
    return value == null ? 0 : value.codePointCount(0, value.length());
  }

  private boolean isBotCommand(String message) {
    String text = trimToNull(message);
    if (text == null) {
      return false;
    }
    return text.startsWith("/") || text.startsWith("!") || text.startsWith("！") || text.startsWith(".");
  }

  private String noticeIdFrom(NapCatResponse response) {
    Object data = response.data();
    if (data instanceof Map<?, ?> map) {
      return idString(firstPresent(toStringKeyMap(map), "notice_id", "noticeId", "id"));
    }
    return idString(data);
  }

  private String findAnnouncementNoticeId(String groupId, Map<String, Object> announcement) {
    String title = trimToNull(announcement.get("title"));
    String content = trimToNull(announcement.get("content"));
    if (title == null || content == null) {
      return null;
    }
    NapCatResponse response = napCatClient.getGroupNotices(groupId);
    if (!response.ok()) {
      return null;
    }
    String expected = formatAnnouncement(title, content);
    for (Map<String, Object> notice : asMapList(response.data())) {
      String noticeId = idString(firstPresent(notice, "notice_id", "noticeId", "id"));
      if (noticeId == null) {
        continue;
      }
      String noticeText =
          firstNonBlank(
              text(firstPresent(notice, "message", "content", "text", "notice_text", "noticeText")),
              text(firstPresent(notice, "title")));
      if (expected.equals(noticeText) || (noticeText.contains(title) && noticeText.contains(content))) {
        return noticeId;
      }
    }
    return null;
  }

  private void logOperation(String module, String action, String targetId, String content) {
    jdbcTemplate.update(
        """
        INSERT INTO operation_log (operator_id, module, action, target_id, content, created_at)
        VALUES (?, 'bot_group_management', ?, ?, ?, CURRENT_TIMESTAMP)
        """,
        currentUserId(),
        module + "/" + action,
        targetId,
        content);
  }

  private int count(String sql) {
    Number number = jdbcTemplate.queryForObject(sql, Number.class);
    return number == null ? 0 : number.intValue();
  }

  private List<Map<String, Object>> queryForList(String sql, Object... args) {
    return jdbcTemplate.queryForList(sql, args);
  }

  private Map<String, Object> queryForOptionalMap(String sql, Object... args) {
    try {
      return jdbcTemplate.queryForMap(sql, args);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  private List<Map<String, Object>> asMapList(Object value) {
    if (value instanceof Collection<?> collection) {
      List<Map<String, Object>> rows = new ArrayList<>();
      for (Object item : collection) {
        if (item instanceof Map<?, ?> map) {
          rows.add(toStringKeyMap(map));
        }
      }
      return rows;
    }
    if (value instanceof Map<?, ?> map) {
      Object list = firstPresent(toStringKeyMap(map), "list", "items", "groups", "members", "notices", "data");
      if (list != value) {
        return asMapList(list);
      }
    }
    return List.of();
  }

  private Map<String, Object> toStringKeyMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() != null) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }
    return result;
  }

  private Object firstPresent(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      if (map.containsKey(key) && map.get(key) != null) {
        return map.get(key);
      }
    }
    return null;
  }

  private String eventNickname(Map<String, Object> event) {
    String nickname =
        trimToNull(firstPresent(event, "nickname", "card", "sender_nickname", "senderNickname", "user_name", "userName"));
    if (nickname != null) {
      return nickname;
    }
    Object sender = event.get("sender");
    if (sender instanceof Map<?, ?> map) {
      return trimToNull(firstPresent(toStringKeyMap(map), "card", "nickname", "user_name", "userName"));
    }
    return null;
  }

  private String idString(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return String.valueOf(number.longValue());
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String text = trimToNull(value);
      if (text != null) {
        return text;
      }
    }
    return "";
  }

  private String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private String trimToNull(Object value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    text = text.trim();
    return text.isEmpty() ? null : text;
  }

  private boolean toBoolean(Object value, boolean defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }
    String text = String.valueOf(value).trim().toLowerCase();
    if (text.isEmpty()) {
      return defaultValue;
    }
    return List.of("true", "1", "yes", "y", "on", "启用").contains(text);
  }

  private Integer toInteger(Object value, Integer defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.valueOf(String.valueOf(value).trim());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private List<String> asStringList(Object value) {
    if (value instanceof Collection<?> collection) {
      return collection.stream().map(this::idString).filter(item -> item != null && !item.isBlank()).toList();
    }
    String text = trimToNull(value);
    if (text == null) {
      return List.of();
    }
    return List.of(text.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
  }

  private String jsonOf(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String text) {
      String trimmed = text.trim();
      return trimmed.isEmpty() ? null : trimmed;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
  }

  private String escapeCqValue(String value) {
    return value.replace("&", "&amp;").replace("[", "&#91;").replace("]", "&#93;").replace(",", "&#44;");
  }

  private Map<String, Object> parseJsonMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return toStringKeyMap(map);
    }
    String text = trimToNull(value);
    if (text == null) {
      return Map.of();
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> result = objectMapper.readValue(text, Map.class);
      return result;
    } catch (JsonProcessingException ex) {
      return Map.of();
    }
  }

  private boolean keywordsMatch(Object value, String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    List<String> keywords = new ArrayList<>();
    if (value instanceof Collection<?> collection) {
      keywords.addAll(collection.stream().map(this::trimToNull).filter(item -> item != null && !item.isBlank()).toList());
    } else {
      String raw = trimToNull(value);
      if (raw != null && raw.startsWith("[")) {
        try {
          @SuppressWarnings("unchecked")
          List<Object> parsed = objectMapper.readValue(raw, List.class);
          keywords.addAll(parsed.stream().map(this::trimToNull).filter(item -> item != null && !item.isBlank()).toList());
        } catch (JsonProcessingException ex) {
          keywords.add(raw);
        }
      } else if (raw != null) {
        keywords.addAll(List.of(raw.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList());
      }
    }
    return keywords.stream().anyMatch(text::contains);
  }

  private String normalizeRole(String value) {
    if (value == null) {
      return "member";
    }
    return switch (value) {
      case "owner", "群主" -> "owner";
      case "admin", "administrator", "管理员" -> "admin";
      default -> "member";
    };
  }

  private Timestamp oneBotTimestamp(Object value) {
    Integer seconds = toInteger(value, null);
    if (seconds == null || seconds <= 0) {
      return null;
    }
    return Timestamp.from(Instant.ofEpochSecond(seconds.longValue()));
  }

  private Timestamp timestampFromValue(Object value) {
    Timestamp timestamp = oneBotTimestamp(value);
    return timestamp == null ? timestampFromText(value) : timestamp;
  }

  private Timestamp timestampFromText(Object value) {
    String text = trimToNull(value);
    if (text == null) {
      return null;
    }
    try {
      return Timestamp.valueOf(text.replace("T", " "));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private LocalDate parseDate(String value, LocalDate defaultValue) {
    String text = trimToNull(value);
    if (text == null) {
      return defaultValue;
    }
    try {
      return LocalDate.parse(text);
    } catch (RuntimeException ex) {
      return defaultValue;
    }
  }

  private Integer currentUserId() {
    try {
      return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
    } catch (RuntimeException ex) {
      return null;
    }
  }
}
