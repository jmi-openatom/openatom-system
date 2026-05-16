package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUserDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestResetPasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUserStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUserUpdateDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseAvatarHealthVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
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
  Result<PageDataVO<User>> getUsers(
      String keyword, UserStatus status, Integer clubId, Long page, Long pageSize);

  Result<String> createUser(RequestCreateUserDTO requestCreateUserDTO);

  Result<String> importUsers(MultipartFile file);

  byte[] exportTemplate();

  Result<User> infoByUserId(Integer userId);

  Result<String> updateUserInfo(Integer userId, RequestUserUpdateDTO requestUserUpdate);

  Result<String> updateUserStatus(
      Integer userId, RequestUpdateUserStatusDTO requestUpdateUserStatusDTO);

  Result<String> resetPassword(
      Integer userId, RequestResetPasswordDTO requestResetPasswordDTO);

  Result<List<ResponseMembershipVO>> getUserMemberships(Integer userId);

  Result<String> deleteUser(Integer userId);

  Result<ResponseAvatarHealthVO> getAvatarHealth();

  Result<Integer> cleanupInvalidAvatars();
}
