package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRecruitmentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubVicePresident;
import java.util.List;

public interface ClubService {
  Result<List<Club>> getClubs(
      String keyword, String category, String status, String recruitmentStatus);

  Result<String> createClub(RequestCreateClubDTO requestCreateClubDTO);

  Result<Club> getClubById(Integer clubId);

  Result<String> updateClub(Integer clubId, RequestUpdateClubDTO requestUpdateClubDTO);

  Result<String> updateStatus(
      Integer clubId, RequestUpdateClubStatusDTO requestUpdateClubStatusDTO);

  Result<String> updateRecruitmentStatus(
      Integer clubId, RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO);

  Result<List<ClubVicePresident>> getVicePresidents(Integer clubId);

  Result<String> addVicePresident(Integer clubId, Integer userId);

  Result<String> removeVicePresident(Integer clubId, Integer userId);
}
