package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.bootstrap.RoleSeedTemplate;
import edu.jmi.openatom.server.openatomsystem.common.web.ClientIpResolver;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangePassword;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseCurrentUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseLoginDTO;
import edu.jmi.openatom.server.openatomsystem.entity.*;
import edu.jmi.openatom.server.openatomsystem.mapper.*;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.AuthService;
import edu.jmi.openatom.server.openatomsystem.service.RegistrationSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final RoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final PermissionMapper permissionMapper;
  private final ClubMapper clubMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final LoginLogMapper loginLogMapper;
  private final PasswordService passwordService;
  private final ClientIpResolver clientIpResolver;
  private final RegistrationSettingService registrationSettingService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public ApiResponse<String> register(RequestRegisterDTO requestRegisterDTO) {
    if (requestRegisterDTO == null
        || requestRegisterDTO.getUsername() == null
        || requestRegisterDTO.getPassword() == null) {
      return ApiResponse.error("注册信息不能为空");
    }
    if (!registrationSettingService.isRegisterEnabled()) {
      return ApiResponse.error(403, "当前未开放注册");
    }
    String studentId =
        requestRegisterDTO.getStudentId() == null || requestRegisterDTO.getStudentId().isBlank()
            ? requestRegisterDTO.getUsername()
            : requestRegisterDTO.getStudentId();
    Long count = userMapper.countByUsernameOrStudentId(requestRegisterDTO.getUsername(), studentId);
    if (count > 0) {
      return ApiResponse.error("该学号已被注册");
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
      return ApiResponse.error("注册失败");
    }
    ApiResponse<String> bindResult = bindProbationaryMemberRole(newUser.getId());
    if (bindResult.getCode() != ApiResponse.SUCCESS_CODE) {
      return bindResult;
    }
    ApiResponse<String> membershipResult = bindDefaultClubMembership(newUser.getId());
    if (membershipResult.getCode() != ApiResponse.SUCCESS_CODE) {
      return membershipResult;
    }
    return ApiResponse.success("注册成功");
  }

  @Override
  public ApiResponse<ResponseLoginDTO> login(RequestLoginDTO requestLoginDTO) {
    if (requestLoginDTO == null || requestLoginDTO.getUsername() == null || requestLoginDTO.getPassword() == null) {
      return ApiResponse.error("用户名或密码不能为空");
    }
    User user = userMapper.selectByStudentIdOrUserName(requestLoginDTO.getUsername());
    if (user == null) {
      return ApiResponse.error("登陆失败,请检查用户名/密码");
    }
    if (!passwordService.matches(requestLoginDTO.getPassword(), user.getPassword())) {
      return ApiResponse.error("登陆失败,请检查用户名/密码");
    }
    upgradePasswordIfNecessary(user, requestLoginDTO.getPassword());
    recordLoginLog(user.getId());
    return ApiResponse.success(createLoginResponse(user), "登陆成功");
  }

  @Override
  public ApiResponse<ResponseLoginDTO> refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return ApiResponse.error("refreshToken不能为空");
    }
    SaTokenDao tokenDao = SaManager.getSaTokenDao();
    String loginId = tokenDao.get(getRefreshTokenKey(refreshToken));
    if (loginId == null) {
      return ApiResponse.error(401, "refreshToken无效或已过期");
    }
    User user = userMapper.selectById(Integer.valueOf(loginId));
    if (user == null) {
      tokenDao.delete(getRefreshTokenKey(refreshToken));
      tokenDao.delete(getRefreshUserKey(loginId));
      return ApiResponse.error(401, "用户不存在");
    }
    tokenDao.delete(getRefreshTokenKey(refreshToken));
    tokenDao.delete(getRefreshUserKey(loginId));
    return ApiResponse.success(createLoginResponse(user), "刷新成功");
  }

  @Override
  public ApiResponse<String> logout(String refreshToken) {
    SaTokenDao tokenDao = SaManager.getSaTokenDao();
    if (StpUtil.isLogin()) {
      String loginId = StpUtil.getLoginIdAsString();
      clearRefreshTokenByLoginId(loginId, tokenDao);
      StpUtil.logout();
      return ApiResponse.success("退出成功");
    }
    if (refreshToken != null && !refreshToken.isBlank()) {
      String loginId = tokenDao.get(getRefreshTokenKey(refreshToken));
      clearRefreshTokenByValue(refreshToken, tokenDao);
      if (loginId != null) {
        tokenDao.delete(getRefreshUserKey(loginId));
      }
      return ApiResponse.success("退出成功");
    }
    return ApiResponse.error(401, "当前未登录");
  }

  @Override
  public ApiResponse<ResponseCurrentUserDTO> getCurrentUserInfo() {
    Integer userId = StpUtil.getLoginIdAsInt();
    User user = userMapper.selectById(userId);
    if (user == null) {
      return ApiResponse.error(404, "用户不存在");
    }
    AuthSnapshot snapshot = buildAuthSnapshot(userId);
    return ApiResponse.success(
        ResponseCurrentUserDTO.builder().user(buildSafeUser(user))
            .roles(snapshot.roles()).permissions(snapshot.permissions()).build());
  }

  @Override
  public ApiResponse<Boolean> updateRegisterEnabled(Boolean enabled) {
    if (enabled == null) return ApiResponse.error(400, "注册开关不能为空");
    if (!StpUtil.isLogin()) return ApiResponse.error(401, "请先登录");
    if (!StpUtil.hasRole("super_admin")) return ApiResponse.error(403, "仅系统管理员可修改注册开关");
    return ApiResponse.success(registrationSettingService.updateRegisterEnabled(enabled), "注册开关更新成功");
  }

  @Override
  public ApiResponse<String> changePassword(RequestChangePassword requestChangePassword) {
    if (requestChangePassword == null || requestChangePassword.getOldPassword() == null
        || requestChangePassword.getNewPassword() == null) {
      return ApiResponse.error("密码不能为空");
    }
    if (!StpUtil.isLogin()) return ApiResponse.error(401, "请先登录");
    int id = StpUtil.getLoginIdAsInt();
    User user = userMapper.selectById(id);
    if (!passwordService.matches(requestChangePassword.getOldPassword(), user.getPassword())) {
      return ApiResponse.error("旧密码不正确");
    }
    user.setPassword(passwordService.encode(requestChangePassword.getNewPassword()));
    userMapper.updateById(user);
    StpUtil.logout();
    return ApiResponse.success("密码更新成功");
  }

  private ResponseLoginDTO createLoginResponse(User user) {
    StpUtil.login(user.getId(), new SaLoginModel());
    user.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
    userMapper.updateById(user);
    String accessToken = StpUtil.getTokenValue();
    String refreshToken = rotateRefreshToken(user.getId());
    AuthSnapshot snapshot = buildAuthSnapshot(user.getId());
    return ResponseLoginDTO.builder().accessToken(accessToken).refreshToken(refreshToken)
        .expiresIn((int) Math.min(StpUtil.getTokenTimeout(), Integer.MAX_VALUE))
        .user(buildSafeUser(user)).roles(snapshot.roles()).permissions(snapshot.permissions()).build();
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

  private void upgradePasswordIfNecessary(User user, String rawPassword) {
    if (passwordService.shouldUpgrade(user.getPassword())) {
      user.setPassword(passwordService.encode(rawPassword));
      userMapper.updateById(user);
    }
  }

  private ApiResponse<String> bindProbationaryMemberRole(Integer userId) {
    String roleCode = RoleSeedTemplate.probationaryMember().code();
    Role role = roleMapper.selectByCode(roleCode);
    if (role == null) return ApiResponse.error(500, "默认非正式成员角色未初始化");
    UserRole exists = userRoleMapper.selectOneByUserAndRole(userId, role.getId());
    if (exists == null) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(role.getId()).build());
    }
    return ApiResponse.success();
  }

  private ApiResponse<String> bindDefaultClubMembership(Integer userId) {
    Club club = clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
    if (club == null) return ApiResponse.error(500, "默认社团未初始化");
    ClubMembership exists = clubMembershipMapper.selectActiveMembership(userId, club.getId());
    if (exists == null) {
      clubMembershipMapper.insert(
          ClubMembership.builder().userId(userId).clubId(club.getId()).status("probation").build());
    }
    return ApiResponse.success();
  }

  private User buildSafeUser(User user) {
    return User.builder().id(user.getId()).userName(user.getUserName()).realName(user.getRealName())
        .gender(user.getGender()).phone(user.getPhone()).email(user.getEmail())
        .studentId(user.getStudentId()).college(user.getCollege()).major(user.getMajor())
        .grade(user.getGrade()).avatar(user.getAvatar()).userStatus(user.getUserStatus())
        .createTime(user.getCreateTime()).lastLoginAt(user.getLastLoginAt()).build();
  }

  private String getRefreshTokenKey(String refreshToken) { return REFRESH_TOKEN_KEY_PREFIX + refreshToken; }
  private String getRefreshUserKey(String loginId) { return REFRESH_USER_KEY_PREFIX + loginId; }

  private record AuthSnapshot(List<String> roles, List<String> permissions) {}
}
