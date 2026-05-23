package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import java.util.List;
import java.util.Map;

public interface BotManagementService {
  Result<Map<String, Object>> overview();

  Result<List<Map<String, Object>>> accounts();

  Result<String> handleOneBotEvent(Map<String, Object> request);

  Result<String> saveAccount(Map<String, Object> request);

  Result<List<Map<String, Object>>> groups(String keyword, Boolean botEnabled, String mode);

  Result<Map<String, Object>> syncGroups();

  Result<Map<String, Object>> groupDetail(String groupId);

  Result<List<Map<String, Object>>> members(String groupId, String keyword, String role, String muteStatus);

  Result<Map<String, Object>> syncMembers(String groupId);

  Result<String> updateGroupConfig(String groupId, Map<String, Object> request);

  Result<String> batchUpdateGroupConfig(Map<String, Object> request);

  Result<String> muteMember(String groupId, String userId, Map<String, Object> request);

  Result<String> muteAll(String groupId, Map<String, Object> request);

  Result<Map<String, Object>> publishAnnouncement(String groupId, Map<String, Object> request);

  Result<List<Map<String, Object>>> announcements(String groupId);

  Result<String> deleteAnnouncement(String groupId, Integer announcementId);

  Result<Map<String, Object>> republishAnnouncement(String groupId, Integer announcementId);

  Result<List<Map<String, Object>>> joinRequests(String groupId, String status);

  Result<Map<String, Object>> saveJoinRequest(String groupId, Map<String, Object> request);

  Result<String> handleJoinRequest(String groupId, Integer requestId, Map<String, Object> request);

  Result<List<Map<String, Object>>> sensitiveWords();

  Result<String> saveSensitiveWord(Map<String, Object> request);

  Result<String> updateSensitiveWord(Integer wordId, Map<String, Object> request);

  Result<String> deleteSensitiveWord(Integer wordId);

  Result<List<Map<String, Object>>> autoReviewRules(String groupId);

  Result<String> saveAutoReviewRule(Map<String, Object> request);

  Result<String> updateAutoReviewRule(Integer ruleId, Map<String, Object> request);

  Result<String> deleteAutoReviewRule(Integer ruleId);

  Result<List<Map<String, Object>>> statistics(String groupId, String startDate, String endDate);

  Result<List<Map<String, Object>>> activeGroups(String startDate, String endDate);

  Result<String> recordMessageStat(Map<String, Object> request);
}
