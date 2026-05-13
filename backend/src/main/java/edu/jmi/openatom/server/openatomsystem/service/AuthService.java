package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestChangePasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestMiniappLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCurrentUserVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;

/**
 * 认证鉴权服务接口
 *
 * <p>定义用户注册, 登录, 刷新令牌, 登出, 获取当前用户信息, 修改密码以及注册开关管理等业务操作
 */
public interface AuthService {
  Result<String> register(RequestRegisterDTO requestRegisterDTO);

  Result<ResponseLoginVO> login(RequestLoginDTO requestLoginDTO);

  Result<ResponseLoginVO> miniappLogin(RequestMiniappLoginDTO requestMiniappLoginDTO);

  Result<String> bindMiniapp(RequestMiniappLoginDTO requestMiniappLoginDTO);

  Result<ResponseLoginVO> refreshToken(String refreshToken);

  Result<String> logout(String refreshToken);

  Result<ResponseCurrentUserVO> getCurrentUserInfo();

  Result<Boolean> updateRegisterEnabled(Boolean enabled);

  Result<String> changePassword(RequestChangePasswordDTO requestChangePassword);
}
