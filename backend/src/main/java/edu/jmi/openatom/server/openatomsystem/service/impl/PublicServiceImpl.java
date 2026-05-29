package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPublicLoginDTO;
import edu.jmi.openatom.server.openatomsystem.entity.DataOpenApplication;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
import edu.jmi.openatom.server.openatomsystem.service.DataOpenApplicationService;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.service.PublicService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePublicLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static cn.dev33.satoken.SaManager.log;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

	private final UserMapper userMapper;
	private final PasswordService passwordService;
	private final PointService pointService;
	private final DataOpenApplicationService dataOpenApplicationService;

	@Override
	public Result<ResponsePublicLoginVO> login(RequestPublicLoginDTO requestPublicLoginDTO, String apiKey) {
		String resolvedApiKey =
				trimToNull(apiKey) == null && requestPublicLoginDTO != null
						? requestPublicLoginDTO.getApiKey()
						: apiKey;
		Result<DataOpenApplication> applicationResult =
				dataOpenApplicationService.validateApiKey(resolvedApiKey);
		if (applicationResult.getCode() != Result.SUCCESS_CODE) {
			return Result.error(applicationResult.getCode(), applicationResult.getMessage());
		}
		DataOpenApplication application = applicationResult.getData();

		if (requestPublicLoginDTO == null || trimToNull(requestPublicLoginDTO.getUsername()) == null || trimToNull(requestPublicLoginDTO.getPassword()) == null) {
			return Result.error("用户名或密码不能为空");
		}
		User user = userMapper.selectByStudentIdOrUserName(requestPublicLoginDTO.getUsername());
		if (user == null) {
			return Result.error("登陆失败,请检查用户名/密码");
		}
		if (!passwordService.matches(requestPublicLoginDTO.getPassword(), user.getPassword())) {
			return Result.error("登陆失败,请检查用户名/密码");
		}


		upgradePasswordIfNecessary(user, requestPublicLoginDTO.getPassword());

		StpUtil.login(user.getId());
		String accessToken = StpUtil.getTokenValue();

		grantDailyLoginPoints(user.getId(), application.getAppName());
		dataOpenApplicationService.recordUsage(application);


		ResponsePublicLoginVO responsePublicLoginVO = ResponsePublicLoginVO.builder()
				.accessToken(accessToken)
				.expiresIn((int) Math.min(StpUtil.getTokenTimeout(), Integer.MAX_VALUE))
				.user(safeUser(user))
				.applicationId(application.getId())
				.applicationName(application.getAppName())
				.build();

		return Result.success(responsePublicLoginVO, responsePublicLoginVO.getApplicationName() + "登录成功!");
	}

	private void upgradePasswordIfNecessary(User user, String rawPassword) {
		if (passwordService.shouldUpgrade(user.getPassword())) {
			user.setPassword(passwordService.encode(rawPassword));
			userMapper.updateById(user);
		}
	}

	private void grantDailyLoginPoints(Integer userId, String applicationName) {
		try {
			pointService.grantDailyLoginPointsPublic(userId, applicationName);
		} catch (Exception e) {
			log.warn("Grant daily login points failed: userId={}", userId, e);
		}
	}

	private User safeUser(User user) {
		if (user == null) return null;
		User safe = User.builder()
				.id(user.getId())
				.userName(user.getUserName())
				.realName(user.getRealName())
				.gender(user.getGender())
				.phone(user.getPhone())
				.email(user.getEmail())
				.studentId(user.getStudentId())
				.college(user.getCollege())
				.major(user.getMajor())
				.grade(user.getGrade())
				.className(user.getClassName())
				.avatar(user.getAvatar())
				.miniappOpenid(user.getMiniappOpenid())
				.wechatUnionid(user.getWechatUnionid())
				.qqOpenid(user.getQqOpenid())
				.userStatus(user.getUserStatus())
				.createTime(user.getCreateTime())
				.lastLoginAt(user.getLastLoginAt())
				.build();
		safe.setPassword(null);
		return safe;
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
