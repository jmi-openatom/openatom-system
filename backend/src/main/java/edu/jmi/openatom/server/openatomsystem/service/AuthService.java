package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestChangePasswordDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestConfirmQqBindDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestMiniappLoginDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestRegisterDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateProfileDTO;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCurrentUserVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLoginVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseGroupJoinTokenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseGroupJoinVerifyVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseQqBindTokenVO;
import org.springframework.web.multipart.MultipartFile;

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

  Result<ResponseQqBindTokenVO> createQqBindToken();

  Result<String> confirmQqBind(RequestConfirmQqBindDTO requestConfirmQqBindDTO);

  Result<Boolean> isQqBound(String qqOpenid);

  Result<String> unbindQq();

  Result<ResponseLoginVO> refreshToken(String refreshToken);

  Result<String> logout(String refreshToken);

  Result<ResponseCurrentUserVO> getCurrentUserInfo();

  Result<Boolean> updateRegisterEnabled(Boolean enabled);

  Result<String> changePassword(RequestChangePasswordDTO requestChangePassword);

  Result<User> updateAvatar(MultipartFile file, String avatarBaseUrl);

  Result<User> removeAvatar();

  Result<User> updateProfile(RequestUpdateProfileDTO requestUpdateProfileDTO);

  Result<User> completeOnboarding();

  Result<User> activate();

  Result<ResponseGroupJoinTokenVO> createGroupJoinToken();

  Result<ResponseGroupJoinVerifyVO> verifyGroupJoinToken(String token);

  Result<String> confirmGroupJoin(String token);
}
