package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangePassword;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseCurrentUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseLoginDTO;

/**
 * 认证鉴权服务接口
 *
 * <p>定义用户注册, 登录, 刷新令牌, 登出, 获取当前用户信息, 修改密码以及注册开关管理等业务操作
 */
public interface AuthService {
  ApiResponse<String> register(RequestRegisterDTO requestRegisterDTO);

  ApiResponse<ResponseLoginDTO> login(RequestLoginDTO requestLoginDTO);

  ApiResponse<ResponseLoginDTO> refreshToken(String refreshToken);

  ApiResponse<String> logout(String refreshToken);

  ApiResponse<ResponseCurrentUserDTO> getCurrentUserInfo();

  ApiResponse<Boolean> updateRegisterEnabled(Boolean enabled);

  ApiResponse<String> changePassword(RequestChangePassword requestChangePassword);
}
