package edu.jmi.openatom.server.openatomsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.jmi.openatom.server.openatomsystem.entity.SystemSetting;

/**
 * 注册设置服务接口
 *
 * <p>定义查询注册开关状态和更新注册开关等业务操作
 */
public interface RegistrationSettingService extends IService<SystemSetting> {
  boolean isRegisterEnabled();

  boolean updateRegisterEnabled(boolean enabled);
}
