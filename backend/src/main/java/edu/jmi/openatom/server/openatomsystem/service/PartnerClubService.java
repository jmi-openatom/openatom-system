package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSavePartnerClubDTO;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePartnerClubVO;
import java.util.List;

/** 开源伙伴公开展示服务。 */
public interface PartnerClubService {
  Result<List<ResponsePartnerClubVO>> publicList(Boolean featured, Integer limit);

  Result<PageDataVO<PartnerClub>> adminList(
      String keyword, String status, Boolean featured, Long page, Long pageSize);

  Result<PartnerClub> create(RequestSavePartnerClubDTO request);

  Result<PartnerClub> update(Integer partnerClubId, RequestSavePartnerClubDTO request);

  Result<PartnerClub> updateStatus(Integer partnerClubId, String status);

  Result<String> delete(Integer partnerClubId);
}
