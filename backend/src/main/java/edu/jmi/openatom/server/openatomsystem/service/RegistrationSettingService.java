package edu.jmi.openatom.server.openatomsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.jmi.openatom.server.openatomsystem.entity.SystemSetting;

public interface RegistrationSettingService extends IService<SystemSetting> {
  boolean isRegisterEnabled();

  boolean updateRegisterEnabled(boolean enabled);
}
