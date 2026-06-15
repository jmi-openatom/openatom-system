package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubAccessVO;
import java.util.List;

public interface ClubAccessService {
  Result<List<ResponseClubAccessVO>> myClubs();

  boolean isSuperAdmin();

  List<Integer> manageableClubIds();

  Integer requireManageableClub(Integer requestedClubId);

  boolean canManageClub(Integer clubId);

  boolean canAccessClub(Integer clubId);
}
