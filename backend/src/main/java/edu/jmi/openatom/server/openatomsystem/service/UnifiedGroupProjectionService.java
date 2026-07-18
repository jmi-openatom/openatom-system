package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.entity.CheckInGroup;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;

/** Keeps the unified group read model synchronized with the legacy domain tables. */
public interface UnifiedGroupProjectionService {
  void syncDepartment(ClubDepartment department);

  void syncCheckInGroup(CheckInGroup group);

  void syncAlumniGroup(ClubAlumniGroup group);

  void syncClubMemberships(Integer clubId);

  void syncBotGroup(String groupId);

  void syncBotGroups();

  void removeSource(String sourceType, Object sourceId);

  void synchronizeAll();
}
