package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import edu.jmi.openatom.server.openatomsystem.bootstrap.RoleSeedTemplate;
import edu.jmi.openatom.server.openatomsystem.cache.RedisCacheEvict;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.web.ClientIpResolver;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestChangePasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestConfirmQqBindDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestMiniappLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateProfileDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCurrentUserVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseQqBindTokenVO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.AuthService;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.service.RegistrationSettingService;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户认证授权实现类
 *
 * <p>负责用户注册, 登录, 令牌刷新, 退出登录, 密码修改以及用户权限快照的构建等核心认证逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private static final long REFRESH_TOKEN_TIMEOUT_SECONDS = 7 * 24 * 60 * 60L;
	private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
	private static final String REFRESH_TOKEN_KEY_PREFIX = "openatom:refresh:token:";
	private static final String REFRESH_USER_KEY_PREFIX = "openatom:refresh:user:";
	private static final String QQ_BIND_TOKEN_KEY_PREFIX = "openatom:qq-bind:token:";
	private static final long QQ_BIND_TOKEN_TIMEOUT_SECONDS = 10 * 60L;
	private static final String MINIAPP_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

	private final UserMapper userMapper;
	private final UserRoleMapper userRoleMapper;
	private final RoleMapper roleMapper;
	private final RolePermissionMapper rolePermissionMapper;
	private final PermissionMapper permissionMapper;
	private final ClubMapper clubMapper;
	private final ClubMembershipMapper clubMembershipMapper;
	private final LoginLogMapper loginLogMapper;
	private final PasswordService passwordService;
	private final AvatarStorageServiceImpl avatarStorageService;
	private final ClientIpResolver clientIpResolver;
	private final RegistrationSettingService registrationSettingService;
	private final PointService pointService;
	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${app.miniapp.app-id:}")
	private String miniappAppId;

	@Value("${app.miniapp.app-secret:}")
	private String miniappAppSecret;

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisCacheEvict(cacheNames = {"site", "auth"})
	public Result<String> register(RequestRegisterDTO requestRegisterDTO) {
		if (requestRegisterDTO == null
				|| requestRegisterDTO.getUsername() == null
				|| requestRegisterDTO.getPassword() == null) {
			return Result.error("注册信息不能为空");
		}
		if (!registrationSettingService.isRegisterEnabled()) {
			return Result.error(403, "当前未开放注册");
		}
		String studentId =
				requestRegisterDTO.getStudentId() == null || requestRegisterDTO.getStudentId().isBlank()
						? requestRegisterDTO.getUsername()
						: requestRegisterDTO.getStudentId();
		Long count = userMapper.countByUsernameOrStudentId(requestRegisterDTO.getUsername(), studentId);
		if (count > 0) {
			return Result.error("该学号已被注册");
		}
		User newUser = User.builder()
				.userName(requestRegisterDTO.getUsername() == null ? studentId : requestRegisterDTO.getUsername())
				.studentId(studentId)
				.password(passwordService.encode(requestRegisterDTO.getPassword()))
				.realName(requestRegisterDTO.getRealName())
				.phone(requestRegisterDTO.getPhone())
				.email(requestRegisterDTO.getEmail())
				.build();
		int rows = userMapper.insert(newUser);
		if (rows <= 0) {
			return Result.error("注册失败");
		}
		Result<String> bindResult = bindProbationaryMemberRole(newUser.getId());
		if (bindResult.getCode() != Result.SUCCESS_CODE) {
			return bindResult;
		}
		Result<String> membershipResult = bindDefaultClubMembership(newUser.getId());
		if (membershipResult.getCode() != Result.SUCCESS_CODE) {
			return membershipResult;
		}
		return Result.success("注册成功");
	}

	@Override
	public Result<ResponseLoginVO> login(RequestLoginDTO requestLoginDTO) {
		if (requestLoginDTO == null || requestLoginDTO.getUsername() == null || requestLoginDTO.getPassword() == null) {
			return Result.error("用户名或密码不能为空");
		}
		User user = userMapper.selectByStudentIdOrUserName(requestLoginDTO.getUsername());
		if (user == null) {
			return Result.error("登陆失败,请检查用户名/密码");
		}
		if (!passwordService.matches(requestLoginDTO.getPassword(), user.getPassword())) {
			return Result.error("登陆失败,请检查用户名/密码");
		}
		upgradePasswordIfNecessary(user, requestLoginDTO.getPassword());
		recordLoginLog(user.getId());
		grantDailyLoginPoints(user.getId());
		return Result.success(createLoginResponse(user), "登陆成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Result<ResponseLoginVO> miniappLogin(RequestMiniappLoginDTO requestMiniappLoginDTO) {
		if (requestMiniappLoginDTO == null || requestMiniappLoginDTO.getCode() == null
				|| requestMiniappLoginDTO.getCode().isBlank()) {
			return Result.error("小程序登录code不能为空");
		}
		if (miniappAppId == null || miniappAppId.isBlank() || miniappAppSecret == null || miniappAppSecret.isBlank()) {
			return Result.error(503, "小程序快捷登录未配置");
		}
		Map<String, Object> session = requestMiniappSession(requestMiniappLoginDTO.getCode().trim());
		String openid = asString(session.get("openid"));
		if (openid == null) {
			return Result.error(401, asString(session.get("errmsg"), "小程序授权失败"));
		}
		User user = userMapper.selectByMiniappOpenid(openid);
		if (user == null) {
			user = createMiniappUser(openid, asString(session.get("unionid")));
		} else if (user.getWechatUnionid() == null && session.get("unionid") != null) {
			user.setWechatUnionid(asString(session.get("unionid")));
			userMapper.updateById(user);
		}
		recordLoginLog(user.getId());
		grantDailyLoginPoints(user.getId());
		return Result.success(createLoginResponse(user), "微信登录成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Result<String> bindMiniapp(RequestMiniappLoginDTO requestMiniappLoginDTO) {
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录后绑定微信");
		if (requestMiniappLoginDTO == null || requestMiniappLoginDTO.getCode() == null
				|| requestMiniappLoginDTO.getCode().isBlank()) {
			return Result.error("小程序登录code不能为空");
		}
		if (miniappAppId == null || miniappAppId.isBlank() || miniappAppSecret == null || miniappAppSecret.isBlank()) {
			return Result.error(503, "小程序快捷登录未配置");
		}
		Map<String, Object> session = requestMiniappSession(requestMiniappLoginDTO.getCode().trim());
		String openid = asString(session.get("openid"));
		if (openid == null) {
			return Result.error(401, asString(session.get("errmsg"), "小程序授权失败"));
		}
		Integer currentUserId = StpUtil.getLoginIdAsInt();
		User currentUser = userMapper.selectById(currentUserId);
		if (currentUser == null) return Result.error(404, "用户不存在");
		User boundUser = userMapper.selectByMiniappOpenid(openid);
		if (boundUser != null && !boundUser.getId().equals(currentUserId)) {
			return Result.error(409, "该微信已绑定其他账号");
		}
		currentUser.setMiniappOpenid(openid);
		if (currentUser.getWechatUnionid() == null && session.get("unionid") != null) {
			currentUser.setWechatUnionid(asString(session.get("unionid")));
		}
		userMapper.updateById(currentUser);
		return Result.success("微信绑定成功");
	}

	@Override
	public Result<ResponseQqBindTokenVO> createQqBindToken() {
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录后生成QQ绑定码");
		Integer currentUserId = StpUtil.getLoginIdAsInt();
		User currentUser = userMapper.selectById(currentUserId);
		if (currentUser == null) return Result.error(404, "用户不存在");

		String token = generateQqBindToken();
		SaManager.getSaTokenDao()
				.set(getQqBindTokenKey(token), String.valueOf(currentUserId), QQ_BIND_TOKEN_TIMEOUT_SECONDS);
		return Result.success(
				ResponseQqBindTokenVO.builder()
						.token(token)
						.expiresIn((int) QQ_BIND_TOKEN_TIMEOUT_SECONDS)
						.build(),
				"QQ绑定码已生成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisCacheEvict(cacheNames = {"site"})
	public Result<String> confirmQqBind(RequestConfirmQqBindDTO requestConfirmQqBindDTO) {
		if (requestConfirmQqBindDTO == null || isBlank(requestConfirmQqBindDTO.getToken())) {
			return Result.error("绑定码不能为空");
		}
		String qqOpenid = normalizeQqOpenid(
				requestConfirmQqBindDTO == null ? null : requestConfirmQqBindDTO.getQqOpenid());
		if (qqOpenid == null) return Result.error("QQ号格式不正确");

		String token = normalizeQqBindToken(requestConfirmQqBindDTO.getToken());
		if (token == null) return Result.error("绑定码格式不正确");
		SaTokenDao tokenDao = SaManager.getSaTokenDao();
		String userIdValue = tokenDao.get(getQqBindTokenKey(token));
		if (userIdValue == null) {
			return Result.error(401, "绑定码无效或已过期");
		}

		Integer userId;
		try {
			userId = Integer.valueOf(userIdValue);
		} catch (NumberFormatException e) {
			log.warn("Invalid QQ bind token payload: token={}, userIdValue={}", token, userIdValue);
			tokenDao.delete(getQqBindTokenKey(token));
			return Result.error(401, "绑定码无效，请在网页重新生成");
		}

		try {
			User currentUser = userMapper.selectById(userId);
			if (currentUser == null) return Result.error(404, "用户不存在");
			User boundUser = userMapper.selectByQqOpenid(qqOpenid);
			if (boundUser != null && !boundUser.getId().equals(currentUser.getId())) {
				return Result.error(409, "该QQ已绑定其他账号");
			}

			currentUser.setQqOpenid(qqOpenid);
			userMapper.updateById(currentUser);
			tokenDao.delete(getQqBindTokenKey(token));
		} catch (DuplicateKeyException e) {
			return Result.error(409, "该QQ已绑定其他账号");
		} catch (DataAccessException e) {
			log.error("QQ bind database operation failed", e);
			return Result.error(500, "QQ绑定数据库字段未初始化，请确认最新迁移已执行后重试");
		}
		return Result.success("QQ绑定成功");
	}

	@Override
	public Result<Boolean> isQqBound(String qqOpenid) {
		String normalized = normalizeQqOpenid(qqOpenid);
		if (normalized == null) return Result.success(false);
		return Result.success(userMapper.selectByQqOpenid(normalized) != null);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisCacheEvict(cacheNames = {"site"})
	public Result<String> unbindQq() {
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录后解绑QQ");
		Integer currentUserId = StpUtil.getLoginIdAsInt();
		User currentUser = userMapper.selectById(currentUserId);
		if (currentUser == null) return Result.error(404, "用户不存在");
		currentUser.setQqOpenid(null);
		userMapper.updateById(currentUser);
		return Result.success("QQ解绑成功");
	}

	@Override
	public Result<ResponseLoginVO> refreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return Result.error("refreshToken不能为空");
		}
		SaTokenDao tokenDao = SaManager.getSaTokenDao();
		String loginId = tokenDao.get(getRefreshTokenKey(refreshToken));
		if (loginId == null) {
			return Result.error(401, "refreshToken无效或已过期");
		}
		User user = userMapper.selectById(Integer.valueOf(loginId));
		if (user == null) {
			tokenDao.delete(getRefreshTokenKey(refreshToken));
			tokenDao.delete(getRefreshUserKey(loginId));
			return Result.error(401, "用户不存在");
		}
		tokenDao.delete(getRefreshTokenKey(refreshToken));
		tokenDao.delete(getRefreshUserKey(loginId));
		return Result.success(createLoginResponse(user), "刷新成功");
	}

	@Override
	public Result<String> logout(String refreshToken) {
		SaTokenDao tokenDao = SaManager.getSaTokenDao();
		if (StpUtil.isLogin()) {
			String loginId = StpUtil.getLoginIdAsString();
			clearRefreshTokenByLoginId(loginId, tokenDao);
			StpUtil.logout();
			return Result.success("退出成功");
		}
		if (refreshToken != null && !refreshToken.isBlank()) {
			String loginId = tokenDao.get(getRefreshTokenKey(refreshToken));
			clearRefreshTokenByValue(refreshToken, tokenDao);
			if (loginId != null) {
				tokenDao.delete(getRefreshUserKey(loginId));
			}
			return Result.success("退出成功");
		}
		return Result.error(401, "当前未登录");
	}

	@Override
	public Result<ResponseCurrentUserVO> getCurrentUserInfo() {
		Integer userId = StpUtil.getLoginIdAsInt();
		User user = userMapper.selectById(userId);
		if (user == null) {
			return Result.error(404, "用户不存在");
		}
		AuthSnapshot snapshot = buildAuthSnapshot(userId);
		return Result.success(
				ResponseCurrentUserVO.builder().user(buildSafeUser(user))
						.roles(snapshot.roles()).permissions(snapshot.permissions()).build());
	}

	@Override
	public Result<Boolean> updateRegisterEnabled(Boolean enabled) {
		if (enabled == null) return Result.error(400, "注册开关不能为空");
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
		if (!StpUtil.hasRole("super_admin")) return Result.error(403, "仅系统管理员可修改注册开关");
		return Result.success(registrationSettingService.updateRegisterEnabled(enabled), "注册开关更新成功");
	}

	@Override
	public Result<String> changePassword(RequestChangePasswordDTO requestChangePassword) {
		if (requestChangePassword == null || requestChangePassword.getOldPassword() == null
				|| requestChangePassword.getNewPassword() == null) {
			return Result.error("密码不能为空");
		}
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
		int id = StpUtil.getLoginIdAsInt();
		User user = userMapper.selectById(id);
		if (!passwordService.matches(requestChangePassword.getOldPassword(), user.getPassword())) {
			return Result.error("旧密码不正确");
		}
		user.setPassword(passwordService.encode(requestChangePassword.getNewPassword()));
		userMapper.updateById(user);
		StpUtil.logout();
		return Result.success("密码更新成功");
	}

	@Override
	@RedisCacheEvict(cacheNames = {"site"})
	public Result<User> updateAvatar(MultipartFile file, String avatarBaseUrl) {
		if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
		if (avatarBaseUrl == null || avatarBaseUrl.isBlank()) return Result.error(500, "头像地址生成失败");
		int id = StpUtil.getLoginIdAsInt();
		User user = userMapper.selectById(id);
		if (user == null) return Result.error(404, "用户不存在");
		String oldAvatar = user.getAvatar();
		try {
			AvatarStorageServiceImpl.StoredAvatar storedAvatar = avatarStorageService.store(file);
			user.setAvatar(avatarBaseUrl + storedAvatar.getFileName());
			int row = userMapper.updateById(user);
			if (row <= 0) return Result.error("头像更新失败");
			avatarStorageService.deleteByAvatarUrl(oldAvatar);
			return Result.success(buildSafeUser(user), "头像更新成功");
		} catch (IOException e) {
			return Result.error(400, e.getMessage());
		}
	}

  @Override
  @RedisCacheEvict(cacheNames = {"site"})
  public Result<User> removeAvatar() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    int id = StpUtil.getLoginIdAsInt();
    User user = userMapper.selectById(id);
    if (user == null) return Result.error(404, "用户不存在");
    String oldAvatar = user.getAvatar();
    user.setAvatar(null);
    int row = userMapper.updateById(user);
    if (row <= 0) return Result.error("头像移除失败");
    avatarStorageService.deleteByAvatarUrl(oldAvatar);
    return Result.success(buildSafeUser(user), "已恢复默认头像");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site", "auth"})
  public Result<User> updateProfile(RequestUpdateProfileDTO requestUpdateProfileDTO) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    if (requestUpdateProfileDTO == null) return Result.error(400, "资料不能为空");
    int id = StpUtil.getLoginIdAsInt();
    User user = userMapper.selectById(id);
    if (user == null) return Result.error(404, "用户不存在");
    if (requestUpdateProfileDTO.getRealName() != null) {
      user.setRealName(requestUpdateProfileDTO.getRealName());
    }
    if (requestUpdateProfileDTO.getGender() != null) {
      user.setGender(requestUpdateProfileDTO.getGender());
    }
    if (requestUpdateProfileDTO.getPhone() != null) {
      user.setPhone(requestUpdateProfileDTO.getPhone());
    }
    if (requestUpdateProfileDTO.getEmail() != null) {
      user.setEmail(requestUpdateProfileDTO.getEmail());
    }
    if (requestUpdateProfileDTO.getCollege() != null) {
      user.setCollege(requestUpdateProfileDTO.getCollege());
    }
    if (requestUpdateProfileDTO.getMajor() != null) {
      user.setMajor(requestUpdateProfileDTO.getMajor());
    }
    if (requestUpdateProfileDTO.getGrade() != null) {
      user.setGrade(requestUpdateProfileDTO.getGrade());
    }
    if (requestUpdateProfileDTO.getClassName() != null) {
      user.setClassName(requestUpdateProfileDTO.getClassName());
    }
    int row = userMapper.updateById(user);
    if (row <= 0) return Result.error("资料更新失败");
    return Result.success(buildSafeUser(user), "资料更新成功");
  }

  @Override
  @RedisCacheEvict(cacheNames = {"site", "auth"})
  public Result<User> completeOnboarding() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    int id = StpUtil.getLoginIdAsInt();
    User user = userMapper.selectById(id);
    if (user == null) return Result.error(404, "用户不存在");
    if (user.getOnboardingCompletedAt() == null) {
      user.setOnboardingCompletedAt(new Timestamp(System.currentTimeMillis()));
      userMapper.updateById(user);
    }
    return Result.success(buildSafeUser(user), "入社介绍已完成");
  }

	private ResponseLoginVO createLoginResponse(User user) {
		StpUtil.login(user.getId(), new SaLoginModel());
		user.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
		userMapper.updateById(user);
		String accessToken = StpUtil.getTokenValue();
		String refreshToken = rotateRefreshToken(user.getId());
		AuthSnapshot snapshot = buildAuthSnapshot(user.getId());
		return ResponseLoginVO.builder().accessToken(accessToken).refreshToken(refreshToken)
				.expiresIn((int) Math.min(StpUtil.getTokenTimeout(), Integer.MAX_VALUE))
				.user(buildSafeUser(user)).roles(snapshot.roles()).permissions(snapshot.permissions()).build();
	}

	private Map<String, Object> requestMiniappSession(String code) {
		try {
			String url = MINIAPP_SESSION_URL
					+ "?appid=" + encode(miniappAppId)
					+ "&secret=" + encode(miniappAppSecret)
					+ "&js_code=" + encode(code)
					+ "&grant_type=authorization_code";
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return Jsons.parseObject(response.body());
		} catch (Exception e) {
			log.warn("Miniapp login request failed", e);
			return Map.of("errmsg", "小程序授权服务不可用");
		}
	}

	private User createMiniappUser(String openid, String unionid) {
		String username = uniqueMiniappUsername(openid);
		User user = User.builder()
				.userName(username)
				.password(passwordService.encode(UUID.randomUUID().toString()))
				.realName("微信用户")
				.miniappOpenid(openid)
				.wechatUnionid(unionid)
				.build();
		userMapper.insert(user);
		Result<String> roleResult = bindProbationaryMemberRole(user.getId());
		if (roleResult.getCode() != Result.SUCCESS_CODE) throw new IllegalStateException(roleResult.getMessage());
		Result<String> membershipResult = bindDefaultClubMembership(user.getId());
		if (membershipResult.getCode() != Result.SUCCESS_CODE)
			throw new IllegalStateException(membershipResult.getMessage());
		return user;
	}

	private String uniqueMiniappUsername(String openid) {
		String suffix = openid.length() > 18 ? openid.substring(openid.length() - 18) : openid;
		String base = "wx_" + suffix.replaceAll("[^A-Za-z0-9_]", "");
		if (base.length() < 4) base = "wx_user";
		String candidate = base;
		int index = 1;
		while (userMapper.selectByStudentIdOrUserName(candidate) != null) {
			candidate = base + "_" + index++;
		}
		return candidate;
	}

	private AuthSnapshot buildAuthSnapshot(Integer userId) {
		List<UserRole> userRoles = userRoleMapper.selectByUserId(userId);
		List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).distinct().collect(Collectors.toList());
		List<Role> roles = roleIds.isEmpty() ? Collections.emptyList() : roleMapper.selectBatchIds(roleIds);
		List<String> roleCodes = roles.stream().map(Role::getCode).filter(c -> c != null && !c.isBlank()).toList();
		List<RolePermission> rolePermissions =
				roleIds.isEmpty() ? Collections.emptyList() : rolePermissionMapper.selectByRoleIds(roleIds);
		List<Integer> permissionIds = rolePermissions.stream()
				.map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
		List<Permission> permissions =
				permissionIds.isEmpty() ? Collections.emptyList() : permissionMapper.selectBatchIds(permissionIds);
		Set<String> permissionCodes = new LinkedHashSet<>();
		for (Permission permission : permissions) {
			if (permission.getCode() != null && !permission.getCode().isBlank()) {
				permissionCodes.add(permission.getCode());
			}
		}
		return new AuthSnapshot(roleCodes, permissionCodes.stream().toList());
	}

	private String rotateRefreshToken(Integer userId) {
		SaTokenDao tokenDao = SaManager.getSaTokenDao();
		String userKey = getRefreshUserKey(String.valueOf(userId));
		String oldRefreshToken = tokenDao.get(userKey);
		if (oldRefreshToken != null) tokenDao.delete(getRefreshTokenKey(oldRefreshToken));
		String refreshToken = UUID.randomUUID().toString().replace("-", "");
		tokenDao.set(getRefreshTokenKey(refreshToken), String.valueOf(userId), REFRESH_TOKEN_TIMEOUT_SECONDS);
		tokenDao.set(userKey, refreshToken, REFRESH_TOKEN_TIMEOUT_SECONDS);
		return refreshToken;
	}

	private void clearRefreshTokenByLoginId(String loginId, SaTokenDao tokenDao) {
		String userKey = getRefreshUserKey(loginId);
		String refreshToken = tokenDao.get(userKey);
		if (refreshToken != null) tokenDao.delete(getRefreshTokenKey(refreshToken));
		tokenDao.delete(userKey);
	}

	private void clearRefreshTokenByValue(String refreshToken, SaTokenDao tokenDao) {
		tokenDao.delete(getRefreshTokenKey(refreshToken));
	}

	private void recordLoginLog(Integer userId) {
		HttpServletRequest request = null;
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			request = attributes.getRequest();
		}
		loginLogMapper.insert(LoginLog.builder().userId(userId)
				.ip(clientIpResolver.resolve(request))
				.userAgent(request == null ? null : request.getHeader("User-Agent")).build());
	}

	private void grantDailyLoginPoints(Integer userId) {
		try {
			pointService.grantDailyLoginPoints(userId);
		} catch (Exception e) {
			log.warn("Grant daily login points failed: userId={}", userId, e);
		}
	}

	private void upgradePasswordIfNecessary(User user, String rawPassword) {
		if (passwordService.shouldUpgrade(user.getPassword())) {
			user.setPassword(passwordService.encode(rawPassword));
			userMapper.updateById(user);
		}
	}

	private Result<String> bindProbationaryMemberRole(Integer userId) {
		String roleCode = RoleSeedTemplate.probationaryMember().code();
		Role role = roleMapper.selectByCode(roleCode);
		if (role == null) return Result.error(500, "默认非正式成员角色未初始化");
		UserRole exists = userRoleMapper.selectOneByUserAndRole(userId, role.getId());
		if (exists == null) {
			userRoleMapper.insert(UserRole.builder().userId(userId).roleId(role.getId()).build());
		}
		return Result.success();
	}

	private Result<String> bindDefaultClubMembership(Integer userId) {
		Club club = clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
		if (club == null) return Result.error(500, "默认社团未初始化");
		ClubMembership exists = clubMembershipMapper.selectActiveMembership(userId, club.getId());
		if (exists == null) {
			clubMembershipMapper.insert(
					ClubMembership.builder().userId(userId).clubId(club.getId()).status("probation").build());
		}
		return Result.success();
	}

	private User buildSafeUser(User user) {
		if (StringUtils.isNotBlank(user.getQqOpenid()) && user.getAvatar() == null) {
			user.setAvatar("https://q1.qlogo.cn/g?b=qq&nk=" + user.getQqOpenid() + "&s=640");
		}
		return User.builder().id(user.getId()).userName(user.getUserName()).realName(user.getRealName())
				.gender(user.getGender()).phone(user.getPhone()).email(user.getEmail())
				.studentId(user.getStudentId()).college(user.getCollege()).major(user.getMajor())
				.grade(user.getGrade()).className(user.getClassName()).avatar(user.getAvatar())
				.userStatus(user.getUserStatus())
				.miniappOpenid(isBlank(user.getMiniappOpenid()) ? null : "BOUND")
				.qqOpenid(isBlank(user.getQqOpenid()) ? null : user.getQqOpenid())
				.createTime(user.getCreateTime()).lastLoginAt(user.getLastLoginAt())
				.onboardingCompletedAt(user.getOnboardingCompletedAt()).build();
	}

	private String normalizeQqOpenid(String qqOpenid) {
		if (qqOpenid == null) return null;
		String trimmed = qqOpenid.trim();
		if (trimmed.isEmpty() || trimmed.length() > 80) return null;
		return trimmed;
	}

	private String normalizeQqBindToken(String token) {
		if (token == null) return null;
		String trimmed = token.trim().toUpperCase();
		if (!trimmed.matches("[A-Z0-9]{8}")) return null;
		return trimmed;
	}

	private String generateQqBindToken() {
		String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
		String token;
		do {
			String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				int index = Integer.parseInt(random.substring(i * 2, i * 2 + 2), 16) % alphabet.length();
				builder.append(alphabet.charAt(index));
			}
			token = builder.toString();
		} while (SaManager.getSaTokenDao().get(getQqBindTokenKey(token)) != null);
		return token;
	}

	private String getQqBindTokenKey(String token) {
		return QQ_BIND_TOKEN_KEY_PREFIX + token;
	}

	private String encode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private String asString(Object value) {
		return asString(value, null);
	}

	private String asString(Object value, String fallback) {
		String stringValue = value == null ? null : String.valueOf(value);
		return stringValue == null || stringValue.isBlank() ? fallback : stringValue;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String getRefreshTokenKey(String refreshToken) {
		return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
	}

	private String getRefreshUserKey(String loginId) {
		return REFRESH_USER_KEY_PREFIX + loginId;
	}

	private record AuthSnapshot(List<String> roles, List<String> permissions) {
	}
}
