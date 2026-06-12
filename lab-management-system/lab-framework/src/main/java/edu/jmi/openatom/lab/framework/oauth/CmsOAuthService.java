package edu.jmi.openatom.lab.framework.oauth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.framework.auth.LabJwtService;
import edu.jmi.openatom.lab.framework.auth.LabUserViews;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import edu.jmi.openatom.lab.framework.mapper.LabUserMapper;
import edu.jmi.openatom.lab.framework.properties.LabAuthProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CmsOAuthService {
  private final LabAuthProperties authProperties;
  private final RestClient restClient;
  private final LabUserMapper labUserMapper;
  private final LabJwtService jwtService;

  public LabDtos.AuthUrlResponse authorizeUrl() {
    LabAuthProperties.OAuth oauth = authProperties.getOauth();
    String state = UUID.randomUUID().toString().replace("-", "");
    String url =
        oauth.getAuthorizeUrl()
            + "?response_type=code"
            + "&client_id="
            + encode(oauth.getClientId())
            + "&redirect_uri="
            + encode(oauth.getRedirectUri())
            + "&scope="
            + encode(oauth.getScopes())
            + "&state="
            + encode(state);
    return new LabDtos.AuthUrlResponse(url, state);
  }

  @SuppressWarnings("unchecked")
  public LabDtos.LoginResponse callback(String code) {
    Map<String, Object> token = exchangeToken(code);
    String accessToken = String.valueOf(token.get("access_token"));
    if (accessToken == null || accessToken.isBlank() || "null".equals(accessToken)) {
      throw new IllegalArgumentException("CMS OAuth 未返回 access_token");
    }
    Map<String, Object> cmsUser =
        restClient
            .get()
            .uri(authProperties.getOauth().getUserInfoUrl())
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(Map.class);
    if (!booleanValue(cmsUser.get("is_lab_member"))) {
      throw new IllegalArgumentException("您当前非实验室正式成员，请联系管理员在社团管理系统开通权限。");
    }
    LabUser user = upsertLabUser(cmsUser);
    LabJwtService.TokenPair tokenPair = jwtService.createToken(user);
    return new LabDtos.LoginResponse(tokenPair.token(), tokenPair.expiresAt(), LabUserViews.toView(user));
  }

  public LabDtos.LoginResponse devLogin(LabDtos.DevLoginRequest request) {
    if (!authProperties.isDevLoginEnabled()) {
      throw new IllegalArgumentException("当前环境未开启 LMS 开发登录");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("club_user_id", request.clubUserId());
    payload.put("username", request.username());
    payload.put("nickname", request.nickname());
    payload.put("lab_role", request.labRole() == null ? 0 : request.labRole());
    payload.put("is_lab_member", true);
    LabUser user = upsertLabUser(payload);
    LabJwtService.TokenPair tokenPair = jwtService.createToken(user);
    return new LabDtos.LoginResponse(tokenPair.token(), tokenPair.expiresAt(), LabUserViews.toView(user));
  }

  public LabDtos.LabUserView me(Long userId) {
    return LabUserViews.toView(labUserMapper.selectById(userId));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> exchangeToken(String code) {
    LabAuthProperties.OAuth oauth = authProperties.getOauth();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", oauth.getRedirectUri());
    form.add("client_id", oauth.getClientId());
    form.add("client_secret", oauth.getClientSecret());
    return restClient
        .post()
        .uri(oauth.getTokenUrl())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(Map.class);
  }

  private LabUser upsertLabUser(Map<String, Object> cmsUser) {
    Long clubUserId = longValue(first(cmsUser, "club_user_id", "clubUserId", "user_id", "id", "sub"));
    if (clubUserId == null) {
      throw new IllegalArgumentException("CMS 用户信息缺少 club_user_id");
    }
    LabUser user =
        labUserMapper.selectOne(new LambdaQueryWrapper<LabUser>().eq(LabUser::getClubUserId, clubUserId));
    boolean insert = user == null;
    if (insert) {
      user = new LabUser();
      user.setClubUserId(clubUserId);
      user.setReputationScore(100);
      user.setDisabled(false);
      user.setCreatedAt(LocalDateTime.now());
    }
    user.setUsername(string(first(cmsUser, "username", "account", "name")));
    user.setNickname(string(first(cmsUser, "nickname", "nick_name", "display_name", "name")));
    user.setAvatarUrl(string(first(cmsUser, "avatar_url", "avatar", "picture")));
    user.setEmail(string(cmsUser.get("email")));
    user.setPhone(string(first(cmsUser, "phone", "mobile")));
    user.setLabRole(resolveRole(cmsUser.get("lab_role")));
    user.setLastLoginAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    if (insert) {
      labUserMapper.insert(user);
    } else {
      labUserMapper.updateById(user);
    }
    return user;
  }

  private Object first(Map<String, Object> payload, String... keys) {
    for (String key : keys) {
      Object value = payload.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private Integer resolveRole(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null) {
      return 0;
    }
    String role = String.valueOf(value).trim().toLowerCase();
    return switch (role) {
      case "coach", "admin", "owner", "2" -> 2;
      case "assistant", "ta", "manager", "1" -> 1;
      default -> 0;
    };
  }

  private boolean booleanValue(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    return value != null && ("true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value)));
  }

  private Long longValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return Math.abs((long) String.valueOf(value).hashCode());
    }
  }

  private String string(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
