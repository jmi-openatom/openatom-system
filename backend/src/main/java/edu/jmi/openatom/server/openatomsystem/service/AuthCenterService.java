package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseTokenIntrospectionVO;

public interface AuthCenterService {
  Result<ResponseTokenIntrospectionVO> introspect(String token);
}
