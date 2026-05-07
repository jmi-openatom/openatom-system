package edu.jmi.openatom.server.openatomsystem.enums;

import java.util.Arrays;
import java.util.List;

public enum SystemPermission {
  AUTH_LOGOUT("退出登录", "auth:logout", "api", "/auth/logout", "POST"),
  AUTH_ME("查看当前用户信息", "auth:me", "api", "/auth/me", "GET"),
  ROLE_LIST("查询角色列表", "role:list", "api", "/roles", "GET"),
  ROLE_CREATE("创建角色", "role:create", "api", "/roles", "POST"),
  ROLE_DETAIL("获取角色详情", "role:detail", "api", "/roles/{roleId}", "GET"),
  ROLE_UPDATE("更新角色", "role:update", "api", "/roles/{roleId}", "PATCH"),
  ROLE_DELETE("删除角色", "role:delete", "api", "/roles/{roleId}", "DELETE"),
  ROLE_PERMISSION_ASSIGN(
      "分配角色权限", "role:permission:assign", "api", "/roles/{roleId}/permissions", "POST"),
  PERMISSION_LIST("查询权限点列表", "permission:list", "api", "/permissions", "GET"),
  PERMISSION_CREATE("创建权限点", "permission:create", "api", "/permissions", "POST"),
  CLUB_LIST("查询社团列表", "club:list", "api", "/clubs", "GET"),
  CLUB_CREATE("创建社团", "club:create", "api", "/clubs", "POST"),
  CLUB_DETAIL("获取社团详情", "club:detail", "api", "/clubs/{clubId}", "GET"),
  CLUB_UPDATE("更新社团信息", "club:update", "api", "/clubs/{clubId}", "PATCH"),
  CLUB_STATUS_UPDATE("更新社团状态", "club:status:update", "api", "/clubs/{clubId}/status", "PATCH"),
  CLUB_RECRUITMENT_STATUS_UPDATE(
      "更新社团招新状态",
      "club:recruitment-status:update",
      "api",
      "/clubs/{clubId}/recruitment-status",
      "PATCH"),
  DEPARTMENT_LIST("查询部门列表", "department:list", "api", "/clubs/{clubId}/departments", "GET"),
  DEPARTMENT_CREATE("创建部门", "department:create", "api", "/clubs/{clubId}/departments", "POST"),
  DEPARTMENT_DETAIL("获取部门详情", "department:detail", "api", "/departments/{departmentId}", "GET"),
  DEPARTMENT_UPDATE("更新部门", "department:update", "api", "/departments/{departmentId}", "PATCH"),
  DEPARTMENT_DELETE("删除部门", "department:delete", "api", "/departments/{departmentId}", "DELETE"),
  POSITION_LIST("查询岗位列表", "position:list", "api", "/clubs/{clubId}/positions", "GET"),
  POSITION_CREATE("创建岗位", "position:create", "api", "/clubs/{clubId}/positions", "POST"),
  POSITION_DETAIL("获取岗位详情", "position:detail", "api", "/positions/{positionId}", "GET"),
  POSITION_UPDATE("更新岗位", "position:update", "api", "/positions/{positionId}", "PATCH"),
  POSITION_DELETE("删除岗位", "position:delete", "api", "/positions/{positionId}", "DELETE"),
  USER_ROLE_ASSIGN("分配用户角色", "user:role:assign", "api", "/users/{userId}/roles", "POST"),
  USER_LIST("查询用户列表", "user:list", "api", "/users", "GET"),
  USER_CREATE("后台创建用户", "user:create", "api", "/users", "POST"),
  USER_INFO("获取用户详情", "user:info", "api", "/users/{userId}", "GET"),
  USER_UPDATE("更新用户信息", "user:update", "api", "/users/{userId}", "PATCH"),
  USER_DELETE("删除用户", "user:delete", "api", "/users/{userId}", "DELETE"),
  USER_STATUS_UPDATE("更新用户状态", "user:status:update", "api", "/users/{userId}/status", "PATCH"),
  USER_PASSWORD_RESET(
      "重置用户密码", "user:password:reset", "api", "/users/{userId}/reset-password", "POST"),
  USER_IMPORT("导入用户与下载模板", "user:import", "api", "/users/import/**", "POST"),
  USER_MEMBERSHIP_LIST(
      "获取用户社团关系", "user:membership:list", "api", "/users/{userId}/memberships", "GET"),
  RECRUITMENT_CAMPAIGN_LIST(
      "查询招新计划", "recruitment-campaign:list", "api", "/clubs/{clubId}/recruitment-campaigns", "GET"),
  RECRUITMENT_CAMPAIGN_CREATE(
      "创建招新计划",
      "recruitment-campaign:create",
      "api",
      "/clubs/{clubId}/recruitment-campaigns",
      "POST"),
  RECRUITMENT_CAMPAIGN_DETAIL(
      "获取招新计划详情",
      "recruitment-campaign:detail",
      "api",
      "/recruitment-campaigns/{campaignId}",
      "GET"),
  RECRUITMENT_CAMPAIGN_UPDATE(
      "更新招新计划",
      "recruitment-campaign:update",
      "api",
      "/recruitment-campaigns/{campaignId}",
      "PATCH"),
  RECRUITMENT_CAMPAIGN_PUBLISH(
      "发布招新计划",
      "recruitment-campaign:publish",
      "api",
      "/recruitment-campaigns/{campaignId}/publish",
      "POST"),
  RECRUITMENT_CAMPAIGN_CLOSE(
      "关闭招新计划",
      "recruitment-campaign:close",
      "api",
      "/recruitment-campaigns/{campaignId}/close",
      "POST"),
  ACTIVITY_LIST("查询活动列表", "activity:list", "api", "/activities", "GET"),
  ACTIVITY_CREATE("创建活动", "activity:create", "api", "/activities", "POST"),
  ACTIVITY_DETAIL("获取活动详情", "activity:detail", "api", "/activities/{activityId}", "GET"),
  ACTIVITY_UPDATE("更新活动", "activity:update", "api", "/activities/{activityId}", "PATCH"),
  ACTIVITY_DELETE("删除活动", "activity:delete", "api", "/activities/{activityId}", "DELETE"),
  ACTIVITY_REGISTRATION_LIST(
      "查询活动报名",
      "activity-registration:list",
      "api",
      "/activities/{activityId}/registrations",
      "GET"),
  AWARD_LIST("查询获奖经历", "award:list", "api", "/awards", "GET"),
  AWARD_CREATE("创建获奖经历", "award:create", "api", "/awards", "POST"),
  AWARD_UPDATE("更新获奖经历", "award:update", "api", "/awards/{awardId}", "PATCH"),
  AWARD_DELETE("删除获奖经历", "award:delete", "api", "/awards/{awardId}", "DELETE"),
  APPLICATION_LIST("查询入会申请", "application:list", "api", "/applications", "GET"),
  APPLICATION_CREATE("提交入会申请", "application:create", "api", "/applications", "POST"),
  APPLICATION_DETAIL(
      "获取入会申请详情", "application:detail", "api", "/applications/{applicationId}", "GET"),
  APPLICATION_UPDATE(
      "更新入会申请", "application:update", "api", "/applications/{applicationId}", "PATCH"),
  APPLICATION_SUBMIT(
      "提交草稿申请", "application:submit", "api", "/applications/{applicationId}/submit", "POST"),
  APPLICATION_WITHDRAW(
      "撤回入会申请", "application:withdraw", "api", "/applications/{applicationId}/withdraw", "POST"),
  APPROVAL_RECORD_LIST(
      "查询审批记录",
      "approval-record:list",
      "api",
      "/applications/{applicationId}/approval-records",
      "GET"),
  APPLICATION_APPROVE(
      "审批入会申请", "application:approve", "api", "/applications/{applicationId}/approvals", "POST"),
  APPLICATION_BATCH_APPROVE(
      "批量审批入会申请", "application:batch-approve", "api", "/applications/batch-approvals", "POST"),
  INTERVIEW_LIST("查询面试列表", "interview:list", "api", "/interviews", "GET"),
  INTERVIEW_CREATE("创建面试安排", "interview:create", "api", "/interviews", "POST"),
  INTERVIEW_DETAIL("获取面试详情", "interview:detail", "api", "/interviews/{interviewId}", "GET"),
  INTERVIEW_UPDATE("更新面试安排", "interview:update", "api", "/interviews/{interviewId}", "PATCH"),
  INTERVIEW_CONFIRM(
      "确认面试", "interview:confirm", "api", "/interviews/{interviewId}/confirm", "POST"),
  INTERVIEW_FEEDBACK(
      "提交面试反馈", "interview:feedback", "api", "/interviews/{interviewId}/feedback", "POST"),
  INTERVIEW_COMPLETE(
      "完成面试", "interview:complete", "api", "/interviews/{interviewId}/complete", "POST"),
  APPLICATION_FINAL_DECISION(
      "终审决策",
      "application:final-decision",
      "api",
      "/applications/{applicationId}/final-decisions",
      "POST"),
  MEMBERSHIP_LIST("查询成员列表", "membership:list", "api", "/memberships", "GET"),
  MEMBERSHIP_CREATE("新增成员", "membership:create", "api", "/memberships", "POST"),
  MEMBERSHIP_DETAIL("获取成员详情", "membership:detail", "api", "/memberships/{membershipId}", "GET"),
  MEMBERSHIP_UPDATE("更新成员", "membership:update", "api", "/memberships/{membershipId}", "PATCH"),
  MEMBERSHIP_POSITION_ASSIGN(
      "分配成员岗位",
      "membership:position:assign",
      "api",
      "/memberships/{membershipId}/assign-position",
      "POST"),
  MEMBERSHIP_STATUS_CHANGE(
      "修改成员状态",
      "membership:status:change",
      "api",
      "/memberships/{membershipId}/change-status",
      "POST"),
  EXIT_APPLICATION_LIST("查询退社申请", "exit-application:list", "api", "/exit-applications", "GET"),
  EXIT_APPLICATION_CREATE("提交退社申请", "exit-application:create", "api", "/exit-applications", "POST"),
  EXIT_APPLICATION_DETAIL(
      "获取退社申请详情",
      "exit-application:detail",
      "api",
      "/exit-applications/{exitApplicationId}",
      "GET"),
  EXIT_APPLICATION_APPROVE(
      "通过退社申请",
      "exit-application:approve",
      "api",
      "/exit-applications/{exitApplicationId}/approve",
      "POST"),
  EXIT_APPLICATION_REJECT(
      "驳回退社申请",
      "exit-application:reject",
      "api",
      "/exit-applications/{exitApplicationId}/reject",
      "POST"),
  MEMBERSHIP_FORCE_EXIT(
      "强制退社", "membership:force-exit", "api", "/memberships/{membershipId}/force-exit", "POST"),
  NOTIFICATION_LIST("查询通知", "notification:list", "api", "/notifications", "GET"),
  NOTIFICATION_CREATE("发送通知", "notification:create", "api", "/notifications", "POST"),
  NOTIFICATION_READ(
      "标记通知已读", "notification:read", "api", "/notifications/{notificationId}/read", "POST"),
  DOCUMENT_LIST("查询文书列表", "document:list", "api", "/office-documents", "GET"),
  DOCUMENT_CREATE("创建文书", "document:create", "api", "/office-documents", "POST"),
  DOCUMENT_UPDATE("更新文书", "document:update", "api", "/office-documents/{documentId}", "PATCH"),
  DOCUMENT_EXPORT("导出文书", "document:export", "api", "/office-documents/{documentId}/export", "GET"),
  LOG_OPERATION_LIST("查询操作日志", "log:operation:list", "api", "/operation-logs", "GET"),
  LOG_LOGIN_LIST("查询登录日志", "log:login:list", "api", "/login-logs", "GET");

  private final String displayName;
  private final String code;
  private final String type;
  private final String path;
  private final String method;

  SystemPermission(String displayName, String code, String type, String path, String method) {
    this.displayName = displayName;
    this.code = code;
    this.type = type;
    this.path = path;
    this.method = method;
  }

  public static List<String> codes(SystemPermission... permissions) {
    return Arrays.stream(permissions).map(SystemPermission::code).toList();
  }

  public String displayName() {
    return displayName;
  }

  public String code() {
    return code;
  }

  public String type() {
    return type;
  }

  public String path() {
    return path;
  }

  public String method() {
    return method;
  }
}
