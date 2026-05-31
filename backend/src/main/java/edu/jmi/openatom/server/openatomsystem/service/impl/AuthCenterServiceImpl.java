package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseTokenIntrospectionVO;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCenterServiceImpl implements edu.jmi.openatom.server.openatomsystem.service.AuthCenterService {
  private final UserMapper userMapper;

  @Override
  public Result<ResponseTokenIntrospectionVO> introspect(String token) {
    String value = normalizeBearer(token);
    if (value == null) {
      return Result.success(ResponseTokenIntrospectionVO.builder().active(false).build());
    }
    Object loginId = StpUtil.getLoginIdByToken(value);
    if (loginId == null) {
      return Result.success(ResponseTokenIntrospectionVO.builder().active(false).build());
    }
    User user = userMapper.selectById(Integer.valueOf(String.valueOf(loginId)));
    if (user == null) {
      return Result.success(ResponseTokenIntrospectionVO.builder().active(false).build());
    }
    long timeout = StpUtil.getTokenTimeout(value);
    if (timeout <= 0) {
      return Result.success(ResponseTokenIntrospectionVO.builder().active(false).build());
    }
    List<String> roles = safeList(() -> StpUtil.getRoleList(loginId));
    List<String> permissions = safeList(() -> StpUtil.getPermissionList(loginId));
    return Result.success(
        ResponseTokenIntrospectionVO.builder()
            .active(true)
            .sub(String.valueOf(user.getId()))
            .username(user.getUserName())
            .name(user.getRealName())
            .clientId(asString(StpUtil.getExtra(value, "client_id")))
            .scope(asString(StpUtil.getExtra(value, "scope")))
            .expiresIn(timeout)
            .exp(System.currentTimeMillis() / 1000 + timeout)
            .roles(roles)
            .permissions(permissions)
            .build());
  }

  private String normalizeBearer(String token) {
    if (token == null || token.isBlank()) return null;
    String trimmed = token.trim();
    if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
      trimmed = trimmed.substring(7).trim();
    }
    return trimmed.isBlank() ? null : trimmed;
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private List<String> safeList(ListSupplier supplier) {
    try {
      List<String> values = supplier.get();
      return values == null ? Collections.emptyList() : values;
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private interface ListSupplier {
    List<String> get();
  }
}
