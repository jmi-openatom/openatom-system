package edu.jmi.openatom.server.openatomsystem.service;

public interface RegistrationSettingService {
  boolean isRegisterEnabled();

  boolean updateRegisterEnabled(boolean enabled);
}
