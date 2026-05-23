package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.BotManagementService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BotManagementController {
  private final BotManagementService botManagementService;

  @Value("${app.bot.callback-token:}")
  private String botCallbackToken;

  @PostMapping("/bot/qq-events")
  public Result<String> handleOneBotEvent(
      @RequestHeader(value = "X-OpenAtom-Bot-Token", required = false) String token,
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody Map<String, Object> request) {
    if (!callbackTokenMatches(token, authorization, request)) {
      return Result.error(401, "机器人事件令牌不正确");
    }
    return botManagementService.handleOneBotEvent(request);
  }

  private boolean callbackTokenMatches(String token, String authorization, Map<String, Object> request) {
    if (botCallbackToken == null || botCallbackToken.isBlank()) {
      return true;
    }
    return botCallbackToken.equals(trimToken(token))
        || botCallbackToken.equals(trimToken(authorization))
        || botCallbackToken.equals(trimToken(request.get("token")));
  }

  private String trimToken(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    if (text.regionMatches(true, 0, "Bearer ", 0, 7)) {
      text = text.substring(7).trim();
    }
    return text.isEmpty() ? null : text;
  }

  @GetMapping("/bot-management/overview")
  @SaCheckPermission("bot-management:list")
  public Result<Map<String, Object>> overview() {
    return botManagementService.overview();
  }

  @GetMapping("/bot-management/accounts")
  @SaCheckPermission("bot-management:list")
  public Result<List<Map<String, Object>>> accounts() {
    return botManagementService.accounts();
  }

  @PostMapping("/bot-management/accounts")
  @SaCheckPermission("bot-management:config")
  public Result<String> saveAccount(@RequestBody Map<String, Object> request) {
    return botManagementService.saveAccount(request);
  }

  @GetMapping("/bot-management/groups")
  @SaCheckPermission("bot-management:list")
  public Result<List<Map<String, Object>>> groups(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean botEnabled,
      @RequestParam(required = false) String mode) {
    return botManagementService.groups(keyword, botEnabled, mode);
  }

  @PostMapping("/bot-management/groups/sync")
  @SaCheckPermission("bot-management:sync")
  public Result<Map<String, Object>> syncGroups() {
    return botManagementService.syncGroups();
  }

  @PostMapping("/bot-management/groups/batch-config")
  @SaCheckPermission("bot-management:config")
  public Result<String> batchUpdateGroupConfig(@RequestBody Map<String, Object> request) {
    return botManagementService.batchUpdateGroupConfig(request);
  }

  @GetMapping("/bot-management/groups/{groupId}")
  @SaCheckPermission("bot-management:detail")
  public Result<Map<String, Object>> groupDetail(@PathVariable String groupId) {
    return botManagementService.groupDetail(groupId);
  }

  @GetMapping("/bot-management/groups/{groupId}/members")
  @SaCheckPermission("bot-management:members")
  public Result<List<Map<String, Object>>> members(
      @PathVariable String groupId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String muteStatus) {
    return botManagementService.members(groupId, keyword, role, muteStatus);
  }

  @PostMapping("/bot-management/groups/{groupId}/members/sync")
  @SaCheckPermission("bot-management:sync")
  public Result<Map<String, Object>> syncMembers(@PathVariable String groupId) {
    return botManagementService.syncMembers(groupId);
  }

  @PatchMapping("/bot-management/groups/{groupId}/config")
  @SaCheckPermission("bot-management:config")
  public Result<String> updateGroupConfig(
      @PathVariable String groupId, @RequestBody Map<String, Object> request) {
    return botManagementService.updateGroupConfig(groupId, request);
  }

  @PostMapping("/bot-management/groups/{groupId}/members/{userId}/mute")
  @SaCheckPermission("bot-management:mute")
  public Result<String> muteMember(
      @PathVariable String groupId, @PathVariable String userId, @RequestBody Map<String, Object> request) {
    return botManagementService.muteMember(groupId, userId, request);
  }

  @PostMapping("/bot-management/groups/{groupId}/mute-all")
  @SaCheckPermission("bot-management:mute")
  public Result<String> muteAll(@PathVariable String groupId, @RequestBody Map<String, Object> request) {
    return botManagementService.muteAll(groupId, request);
  }

  @GetMapping("/bot-management/groups/{groupId}/announcements")
  @SaCheckPermission("bot-management:announcements")
  public Result<List<Map<String, Object>>> announcements(@PathVariable String groupId) {
    return botManagementService.announcements(groupId);
  }

  @PostMapping("/bot-management/groups/{groupId}/announcements")
  @SaCheckPermission("bot-management:announcements")
  public Result<Map<String, Object>> publishAnnouncement(
      @PathVariable String groupId, @RequestBody Map<String, Object> request) {
    return botManagementService.publishAnnouncement(groupId, request);
  }

  @PostMapping("/bot-management/groups/{groupId}/announcements/{announcementId}/republish")
  @SaCheckPermission("bot-management:announcements")
  public Result<Map<String, Object>> republishAnnouncement(
      @PathVariable String groupId, @PathVariable Integer announcementId) {
    return botManagementService.republishAnnouncement(groupId, announcementId);
  }

  @DeleteMapping("/bot-management/groups/{groupId}/announcements/{announcementId}")
  @SaCheckPermission("bot-management:announcements")
  public Result<String> deleteAnnouncement(
      @PathVariable String groupId, @PathVariable Integer announcementId) {
    return botManagementService.deleteAnnouncement(groupId, announcementId);
  }

  @GetMapping("/bot-management/groups/{groupId}/join-requests")
  @SaCheckPermission("bot-management:join-requests")
  public Result<List<Map<String, Object>>> joinRequests(
      @PathVariable String groupId, @RequestParam(required = false) String status) {
    return botManagementService.joinRequests(groupId, status);
  }

  @PostMapping("/bot-management/groups/{groupId}/join-requests")
  @SaCheckPermission("bot-management:join-requests")
  public Result<Map<String, Object>> saveJoinRequest(
      @PathVariable String groupId, @RequestBody Map<String, Object> request) {
    return botManagementService.saveJoinRequest(groupId, request);
  }

  @PostMapping("/bot-management/groups/{groupId}/join-requests/{requestId}/handle")
  @SaCheckPermission("bot-management:join-requests")
  public Result<String> handleJoinRequest(
      @PathVariable String groupId,
      @PathVariable Integer requestId,
      @RequestBody Map<String, Object> request) {
    return botManagementService.handleJoinRequest(groupId, requestId, request);
  }

  @GetMapping("/bot-management/sensitive-words")
  @SaCheckPermission("bot-management:sensitive-words")
  public Result<List<Map<String, Object>>> sensitiveWords() {
    return botManagementService.sensitiveWords();
  }

  @PostMapping("/bot-management/sensitive-words")
  @SaCheckPermission("bot-management:sensitive-words")
  public Result<String> saveSensitiveWord(@RequestBody Map<String, Object> request) {
    return botManagementService.saveSensitiveWord(request);
  }

  @PatchMapping("/bot-management/sensitive-words/{wordId}")
  @SaCheckPermission("bot-management:sensitive-words")
  public Result<String> updateSensitiveWord(
      @PathVariable Integer wordId, @RequestBody Map<String, Object> request) {
    return botManagementService.updateSensitiveWord(wordId, request);
  }

  @DeleteMapping("/bot-management/sensitive-words/{wordId}")
  @SaCheckPermission("bot-management:sensitive-words")
  public Result<String> deleteSensitiveWord(@PathVariable Integer wordId) {
    return botManagementService.deleteSensitiveWord(wordId);
  }

  @GetMapping("/bot-management/auto-review-rules")
  @SaCheckPermission("bot-management:auto-review")
  public Result<List<Map<String, Object>>> autoReviewRules(@RequestParam(required = false) String groupId) {
    return botManagementService.autoReviewRules(groupId);
  }

  @PostMapping("/bot-management/auto-review-rules")
  @SaCheckPermission("bot-management:auto-review")
  public Result<String> saveAutoReviewRule(@RequestBody Map<String, Object> request) {
    return botManagementService.saveAutoReviewRule(request);
  }

  @PatchMapping("/bot-management/auto-review-rules/{ruleId}")
  @SaCheckPermission("bot-management:auto-review")
  public Result<String> updateAutoReviewRule(
      @PathVariable Integer ruleId, @RequestBody Map<String, Object> request) {
    return botManagementService.updateAutoReviewRule(ruleId, request);
  }

  @DeleteMapping("/bot-management/auto-review-rules/{ruleId}")
  @SaCheckPermission("bot-management:auto-review")
  public Result<String> deleteAutoReviewRule(@PathVariable Integer ruleId) {
    return botManagementService.deleteAutoReviewRule(ruleId);
  }

  @GetMapping("/bot-management/statistics")
  @SaCheckPermission("bot-management:statistics")
  public Result<List<Map<String, Object>>> statistics(
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
    return botManagementService.statistics(startDate, endDate);
  }

  @GetMapping("/bot-management/statistics/active-groups")
  @SaCheckPermission("bot-management:statistics")
  public Result<List<Map<String, Object>>> activeGroups(
      @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
    return botManagementService.activeGroups(startDate, endDate);
  }

  @PostMapping("/bot-management/message-stats")
  @SaCheckPermission("bot-management:statistics")
  public Result<String> recordMessageStat(@RequestBody Map<String, Object> request) {
    return botManagementService.recordMessageStat(request);
  }
}
