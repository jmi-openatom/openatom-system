package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestOauthClientDTO;
import edu.jmi.openatom.server.openatomsystem.entity.OauthClient;
import java.util.List;

public interface OauthClientService {
  Result<List<OauthClient>> list();

  Result<OauthClient> create(RequestOauthClientDTO request);

  Result<OauthClient> update(Integer id, RequestOauthClientDTO request);

  Result<String> delete(Integer id);
}
