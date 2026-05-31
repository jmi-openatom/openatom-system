package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestOauthClientDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import edu.jmi.openatom.server.openatomsystem.service.OauthClientService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth/admin/clients")
public class OauthClientController {
  private final OauthClientService oauthClientService;

  @GetMapping
  @SaCheckPermission("oauth-client:list")
  public Result<List<OauthClient>> list() {
    return oauthClientService.list();
  }

  @PostMapping
  @SaCheckPermission("oauth-client:manage")
  public Result<OauthClient> create(@Valid @RequestBody RequestOauthClientDTO request) {
    return oauthClientService.create(request);
  }

  @PatchMapping("/{id}")
  @SaCheckPermission("oauth-client:manage")
  public Result<OauthClient> update(
      @PathVariable Integer id, @Valid @RequestBody RequestOauthClientDTO request) {
    return oauthClientService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @SaCheckPermission("oauth-client:manage")
  public Result<String> delete(@PathVariable Integer id) {
    return oauthClientService.delete(id);
  }
}
