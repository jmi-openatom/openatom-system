package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangePassword;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseCurrentUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseLoginDTO;

public interface AuthService {
  ApiResponse<String> register(RequestRegisterDTO requestRegisterDTO);

  ApiResponse<ResponseLoginDTO> login(RequestLoginDTO requestLoginDTO);

  ApiResponse<ResponseLoginDTO> refreshToken(String refreshToken);

  ApiResponse<String> logout(String refreshToken);

  ApiResponse<ResponseCurrentUserDTO> getCurrentUserInfo();

  ApiResponse<Boolean> updateRegisterEnabled(Boolean enabled);

  ApiResponse<String> changePassword(RequestChangePassword requestChangePassword);
}
