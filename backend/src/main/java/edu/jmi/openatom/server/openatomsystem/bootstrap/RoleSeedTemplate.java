package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.enums.SystemPermission;

import java.util.List;

/**
 * 角色种子模板
 *
 * <p>定义系统预置角色的模板数据, 包含超级管理员, 社团管理员, 部门负责人, 正式成员, 非正式成员等角色及其权限配置
 */
public record RoleSeedTemplate(
		String code, String name, String dataScope, String description, List<String> permissionCodes) {

	public static RoleSeedTemplate superAdmin() {
		return new RoleSeedTemplate(
				"super_admin", "系统管理员", "all", "拥有系统全部接口权限", PermissionSeedCatalog.allCodes());
	}

	public static RoleSeedTemplate formalMember() {
		return new RoleSeedTemplate(
				"formal_member", "正式成员", "self", "社团正式成员基础角色", formalMemberPermissionCodes());
	}

	public static RoleSeedTemplate clubAdmin() {
		return new RoleSeedTemplate(
				"club_admin", "社团管理员", "club", "管理所属社团基础资料、部门与成员", clubAdminPermissionCodes());
	}

	public static RoleSeedTemplate departmentHead() {
		return new RoleSeedTemplate(
				"department_head", "部门负责人", "department", "管理所属部门基础事务", departmentHeadPermissionCodes());
	}

	public static RoleSeedTemplate probationaryMember() {
		return new RoleSeedTemplate(
				"probationary_member", "非正式成员", "self", "社团非正式成员基础角色", probationaryMemberPermissionCodes());
	}

	public static List<String> clubAdminPermissionCodes() {
		return SystemPermission.codes(
				SystemPermission.AUTH_ME,
				SystemPermission.AUTH_LOGOUT,
				SystemPermission.CLUB_LIST,
				SystemPermission.CLUB_DETAIL,
				SystemPermission.CLUB_UPDATE,
				SystemPermission.CLUB_RECRUITMENT_STATUS_UPDATE,
				SystemPermission.DEPARTMENT_LIST,
				SystemPermission.DEPARTMENT_CREATE,
				SystemPermission.DEPARTMENT_DETAIL,
				SystemPermission.DEPARTMENT_UPDATE,
				SystemPermission.DEPARTMENT_DELETE,
				SystemPermission.POSITION_LIST,
				SystemPermission.POSITION_CREATE,
				SystemPermission.POSITION_DETAIL,
				SystemPermission.POSITION_UPDATE,
				SystemPermission.POSITION_DELETE,
				SystemPermission.RECRUITMENT_CAMPAIGN_LIST,
				SystemPermission.RECRUITMENT_CAMPAIGN_CREATE,
				SystemPermission.RECRUITMENT_CAMPAIGN_DETAIL,
				SystemPermission.RECRUITMENT_CAMPAIGN_UPDATE,
				SystemPermission.RECRUITMENT_CAMPAIGN_PUBLISH,
				SystemPermission.RECRUITMENT_CAMPAIGN_CLOSE,
				SystemPermission.APPLICATION_LIST,
				SystemPermission.APPLICATION_EXPORT,
				SystemPermission.APPLICATION_DETAIL,
				SystemPermission.APPROVAL_RECORD_LIST,
				SystemPermission.APPLICATION_APPROVE,
				SystemPermission.APPLICATION_BATCH_APPROVE,
				SystemPermission.INTERVIEW_LIST,
				SystemPermission.INTERVIEW_CREATE,
				SystemPermission.INTERVIEW_DETAIL,
				SystemPermission.INTERVIEW_UPDATE,
				SystemPermission.INTERVIEW_COMPLETE,
				SystemPermission.APPLICATION_FINAL_DECISION,
				SystemPermission.MEMBERSHIP_LIST,
				SystemPermission.MEMBERSHIP_CREATE,
				SystemPermission.MEMBERSHIP_DETAIL,
				SystemPermission.MEMBERSHIP_UPDATE,
				SystemPermission.MEMBERSHIP_POSITION_ASSIGN,
				SystemPermission.MEMBERSHIP_STATUS_CHANGE,
				SystemPermission.MEMBERSHIP_BATCH_CHANGE_STATUS,
				SystemPermission.MEMBERSHIP_BATCH_CREATE,
				SystemPermission.CHECK_IN_LIST,
				SystemPermission.CHECK_IN_DETAIL,
				SystemPermission.CHECK_IN_CREATE,
				SystemPermission.CHECK_IN_UPDATE,
				SystemPermission.CHECK_IN_DELETE,
				SystemPermission.CHECK_IN_RECORDS,
				SystemPermission.CHECK_IN_GROUP_MEMBER_DELETE,
				SystemPermission.LEAVE_APPLICATION_LIST,
				SystemPermission.LEAVE_APPLICATION_DETAIL,
				SystemPermission.LEAVE_APPLICATION_REVIEW,
				SystemPermission.LEAVE_APPLICATION_DELETE,
				SystemPermission.SCHOOL_CALENDAR_MANAGE,
				SystemPermission.EXIT_APPLICATION_LIST,
				SystemPermission.EXIT_APPLICATION_DETAIL,
				SystemPermission.EXIT_APPLICATION_APPROVE,
				SystemPermission.EXIT_APPLICATION_REJECT,
				SystemPermission.MEMBERSHIP_FORCE_EXIT,
				SystemPermission.DOCUMENT_LIST,
				SystemPermission.DOCUMENT_CREATE,
				SystemPermission.DOCUMENT_UPDATE,
				SystemPermission.DOCUMENT_EXPORT,
				SystemPermission.IMAGE_UPLOAD,
				SystemPermission.IMAGE_LIST,
					SystemPermission.IMAGE_DELETE,
					SystemPermission.DATA_OPEN_LIST,
					SystemPermission.DATA_OPEN_REVIEW,
					SystemPermission.OAUTH_CLIENT_LIST,
					SystemPermission.OAUTH_CLIENT_MANAGE,
					SystemPermission.SITE_FORM_LIST,
				SystemPermission.SITE_FORM_CREATE,
				SystemPermission.SITE_FORM_DETAIL,
				SystemPermission.SITE_FORM_UPDATE,
				SystemPermission.SITE_FORM_EXPORT,
				SystemPermission.LOTTERY_LIST,
				SystemPermission.LOTTERY_CREATE,
				SystemPermission.LOTTERY_DETAIL,
				SystemPermission.LOTTERY_UPDATE,
				SystemPermission.LOTTERY_DRAW,
				SystemPermission.POINT_ACCOUNT_LIST,
				SystemPermission.POINT_TRANSACTION_LIST,
				SystemPermission.POINT_ADJUST,
				SystemPermission.POINT_ITEM_LIST,
				SystemPermission.POINT_ITEM_MANAGE,
				SystemPermission.POINT_REDEMPTION_LIST,
				SystemPermission.POINT_REDEMPTION_MANAGE,
				SystemPermission.BLOG_LIST,
				SystemPermission.BLOG_REVIEW,
				SystemPermission.BLOG_DELETE,
				SystemPermission.BLOG_COMMENT_LIST,
				SystemPermission.BLOG_COMMENT_MANAGE,
				SystemPermission.BLOG_INTERACTION_LIST,
				SystemPermission.NOTIFICATION_LIST,
				SystemPermission.NOTIFICATION_CREATE,
				SystemPermission.NOTIFICATION_DELETE,
				SystemPermission.BOT_MANAGEMENT_LIST,
				SystemPermission.BOT_MANAGEMENT_DETAIL,
				SystemPermission.BOT_MANAGEMENT_MEMBERS,
				SystemPermission.BOT_MANAGEMENT_SYNC,
				SystemPermission.BOT_MANAGEMENT_CONFIG,
				SystemPermission.BOT_MANAGEMENT_MUTE,
				SystemPermission.BOT_MANAGEMENT_MESSAGES,
				SystemPermission.BOT_MANAGEMENT_ANNOUNCEMENTS,
				SystemPermission.BOT_MANAGEMENT_JOIN_REQUESTS,
				SystemPermission.BOT_MANAGEMENT_SENSITIVE_WORDS,
				SystemPermission.BOT_MANAGEMENT_AUTO_REVIEW,
				SystemPermission.BOT_MANAGEMENT_STATISTICS);
	}

	public static List<String> departmentHeadPermissionCodes() {
		return SystemPermission.codes(
				SystemPermission.AUTH_ME,
				SystemPermission.AUTH_LOGOUT,
				SystemPermission.CLUB_LIST,
				SystemPermission.CLUB_DETAIL,
				SystemPermission.DEPARTMENT_LIST,
				SystemPermission.DEPARTMENT_DETAIL,
				SystemPermission.POSITION_LIST,
				SystemPermission.POSITION_DETAIL,
				SystemPermission.APPLICATION_LIST,
				SystemPermission.APPLICATION_DETAIL,
				SystemPermission.APPROVAL_RECORD_LIST,
				SystemPermission.APPLICATION_APPROVE,
				SystemPermission.INTERVIEW_LIST,
				SystemPermission.INTERVIEW_CREATE,
				SystemPermission.INTERVIEW_DETAIL,
				SystemPermission.INTERVIEW_UPDATE,
				SystemPermission.INTERVIEW_FEEDBACK,
				SystemPermission.INTERVIEW_COMPLETE,
				SystemPermission.MEMBERSHIP_LIST,
				SystemPermission.MEMBERSHIP_DETAIL,
				SystemPermission.MEMBERSHIP_UPDATE,
				SystemPermission.IMAGE_UPLOAD,
				SystemPermission.IMAGE_LIST,
				SystemPermission.EXIT_APPLICATION_LIST,
				SystemPermission.EXIT_APPLICATION_DETAIL);
	}

	public static List<String> formalMemberPermissionCodes() {
		return SystemPermission.codes(
				SystemPermission.AUTH_ME,
				SystemPermission.AUTH_LOGOUT,
				SystemPermission.CLUB_LIST,
				SystemPermission.CLUB_DETAIL,
				SystemPermission.USER_INFO,
				SystemPermission.USER_UPDATE,
				SystemPermission.DEPARTMENT_LIST,
				SystemPermission.DEPARTMENT_DETAIL,
				SystemPermission.POSITION_LIST,
				SystemPermission.POSITION_DETAIL,
				SystemPermission.RECRUITMENT_CAMPAIGN_LIST,
				SystemPermission.RECRUITMENT_CAMPAIGN_DETAIL,
				SystemPermission.APPLICATION_LIST,
				SystemPermission.APPLICATION_CREATE,
				SystemPermission.APPLICATION_DETAIL,
				SystemPermission.APPLICATION_UPDATE,
				SystemPermission.APPLICATION_SUBMIT,
				SystemPermission.APPLICATION_WITHDRAW,
				SystemPermission.INTERVIEW_LIST,
				SystemPermission.INTERVIEW_DETAIL,
				SystemPermission.INTERVIEW_CONFIRM,
				SystemPermission.EXIT_APPLICATION_CREATE,
				SystemPermission.EXIT_APPLICATION_DETAIL,
				SystemPermission.IMAGE_UPLOAD,
				SystemPermission.IMAGE_LIST,
				SystemPermission.BLOG_MY_LIST,
				SystemPermission.BLOG_CREATE,
				SystemPermission.BLOG_UPDATE_OWN,
				SystemPermission.BLOG_DELETE_OWN,
				SystemPermission.NOTIFICATION_LIST,
				SystemPermission.NOTIFICATION_READ);
	}

	public static List<String> probationaryMemberPermissionCodes() {
		return SystemPermission.codes(
				SystemPermission.AUTH_ME,
				SystemPermission.AUTH_LOGOUT,
				SystemPermission.CLUB_LIST,
				SystemPermission.CLUB_DETAIL,
				SystemPermission.USER_INFO,
				SystemPermission.DEPARTMENT_LIST,
				SystemPermission.DEPARTMENT_DETAIL,
				SystemPermission.POSITION_LIST,
				SystemPermission.POSITION_DETAIL,
				SystemPermission.RECRUITMENT_CAMPAIGN_LIST,
				SystemPermission.RECRUITMENT_CAMPAIGN_DETAIL,
				SystemPermission.APPLICATION_CREATE,
				SystemPermission.APPLICATION_DETAIL,
				SystemPermission.APPLICATION_UPDATE,
				SystemPermission.APPLICATION_SUBMIT,
				SystemPermission.APPLICATION_WITHDRAW,
				SystemPermission.INTERVIEW_LIST,
				SystemPermission.INTERVIEW_DETAIL,
				SystemPermission.INTERVIEW_CONFIRM,
				SystemPermission.IMAGE_UPLOAD,
				SystemPermission.IMAGE_LIST,
				SystemPermission.NOTIFICATION_LIST,
				SystemPermission.NOTIFICATION_READ);
	}
}
