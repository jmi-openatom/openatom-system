package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.enums.SystemPermission;
import java.util.List;

public final class PermissionSeedCatalog {
  private PermissionSeedCatalog() {}

  public static List<PermissionSeed> all() {
    return List.of(SystemPermission.values()).stream()
        .map(
            permission ->
                new PermissionSeed(
                    permission.displayName(),
                    permission.code(),
                    permission.type(),
                    permission.path(),
                    permission.method()))
        .toList();
  }

  public static List<String> allCodes() {
    return all().stream().map(PermissionSeed::code).toList();
  }

  public record PermissionSeed(String name, String code, String type, String path, String method) {}
}
