package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestResetPasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateUserStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUserUpdate;
import edu.jmi.openatom.server.openatomsystem.dto.response.PageDataDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
  ApiResponse<PageDataDTO<User>> getUsers(
      String keyword, UserStatus status, Integer clubId, Long page, Long pageSize);

  ApiResponse<String> createUser(RequestCreateUserDTO requestCreateUserDTO);

  ApiResponse<String> importUsers(MultipartFile file);

  byte[] exportTemplate();

  ApiResponse<User> infoByUserId(Integer userId);

  ApiResponse<String> updateUserInfo(Integer userId, RequestUserUpdate requestUserUpdate);

  ApiResponse<String> updateUserStatus(
      Integer userId, RequestUpdateUserStatusDTO requestUpdateUserStatusDTO);

  ApiResponse<String> resetPassword(
      Integer userId, RequestResetPasswordDTO requestResetPasswordDTO);

  ApiResponse<List<ResponseMembershipDTO>> getUserMemberships(Integer userId);

  ApiResponse<String> deleteUser(Integer userId);
}
