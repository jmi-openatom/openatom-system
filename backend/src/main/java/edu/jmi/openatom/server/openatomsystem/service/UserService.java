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

/**
 * 用户管理服务接口
 *
 * <p>定义用户的分页列表, 创建, 导入, 导出模板, 查看详情, 更新信息, 更新状态, 重置密码, 查询用户社团成员身份和删除等业务操作
 */
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
