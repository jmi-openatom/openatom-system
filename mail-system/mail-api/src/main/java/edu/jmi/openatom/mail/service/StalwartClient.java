package edu.jmi.openatom.mail.service;

import java.util.List;

public interface StalwartClient {
  String ensureAccount(String oauthSub, String displayName, List<String> aliases, long quotaBytes);

  void setEnabled(String accountId, boolean enabled);

  void updateAliases(String accountId, List<String> aliases);
}
