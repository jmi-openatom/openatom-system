package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestOauthClientDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.mapper.OauthClientMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.OauthClientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OauthClientServiceImpl implements OauthClientService {
  private final OauthClientMapper oauthClientMapper;
  private final PasswordService passwordService;

  @Override
  public Result<List<OauthClient>> list() {
    List<OauthClient> clients =
        oauthClientMapper.selectList(
            new LambdaQueryWrapper<OauthClient>().orderByDesc(OauthClient::getCreatedAt));
    clients.forEach(client -> client.setClientSecret(maskSecret(client.getClientSecret())));
    return Result.success(clients);
  }

  @Override
  public Result<OauthClient> create(RequestOauthClientDTO request) {
    if (request == null) return Result.error(400, "请求参数不能为空");
    if (oauthClientMapper.selectByClientId(request.getClientId()) != null) {
      return Result.error(409, "clientId已存在");
    }
    OauthClient client =
        OauthClient.builder()
            .clientId(request.getClientId().trim())
            .clientSecret(encodeSecret(request.getClientSecret()))
            .clientName(request.getClientName().trim())
            .redirectUris(request.getRedirectUris().trim())
            .scopes(defaultIfBlank(request.getScopes(), "openid profile email"))
            .grantTypes(defaultIfBlank(request.getGrantTypes(), "authorization_code refresh_token"))
            .enabled(request.getEnabled() == null || request.getEnabled())
            .build();
    oauthClientMapper.insert(client);
    client.setClientSecret(maskSecret(client.getClientSecret()));
    return Result.success(client, "创建成功");
  }

  @Override
  public Result<OauthClient> update(Integer id, RequestOauthClientDTO request) {
    if (id == null) return Result.error(400, "id不能为空");
    if (request == null) return Result.error(400, "请求参数不能为空");
    OauthClient client = oauthClientMapper.selectById(id);
    if (client == null) return Result.error(404, "客户端不存在");
    OauthClient sameClientId = oauthClientMapper.selectByClientId(request.getClientId());
    if (sameClientId != null && !sameClientId.getId().equals(id)) {
      return Result.error(409, "clientId已存在");
    }
    client.setClientId(request.getClientId().trim());
    if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
      client.setClientSecret(encodeSecret(request.getClientSecret()));
    }
    client.setClientName(request.getClientName().trim());
    client.setRedirectUris(request.getRedirectUris().trim());
    client.setScopes(defaultIfBlank(request.getScopes(), "openid profile email"));
    client.setGrantTypes(defaultIfBlank(request.getGrantTypes(), "authorization_code refresh_token"));
    client.setEnabled(request.getEnabled() == null || request.getEnabled());
    oauthClientMapper.updateById(client);
    client.setClientSecret(maskSecret(client.getClientSecret()));
    return Result.success(client, "更新成功");
  }

  @Override
  public Result<String> delete(Integer id) {
    if (id == null) return Result.error(400, "id不能为空");
    OauthClient client = oauthClientMapper.selectById(id);
    if (client == null) return Result.error(404, "客户端不存在");
    oauthClientMapper.deleteById(id);
    return Result.success("删除成功");
  }

  private String encodeSecret(String rawSecret) {
    if (rawSecret == null || rawSecret.isBlank()) return null;
    return passwordService.encode(rawSecret.trim());
  }

  private String defaultIfBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String maskSecret(String secret) {
    return secret == null || secret.isBlank() ? null : "******";
  }
}
