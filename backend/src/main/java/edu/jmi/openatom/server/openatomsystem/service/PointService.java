package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedeemItemDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRuleSettingsDTO;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointAccountVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedeemItemVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedemptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRuleSettingsVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointSummaryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointTransactionVO;
import java.util.List;

public interface PointService {
  Result<List<ResponsePointAccountVO>> leaderboard(Integer limit);

  Result<ResponsePointSummaryVO> mySummary();

  Result<List<ResponsePointRedeemItemVO>> siteItems();

  Result<Integer> redeem(Integer itemId, RequestPointRedemptionDTO request);

  Result<List<ResponsePointRedemptionVO>> myRedemptions();

  Result<List<ResponsePointAccountVO>> adminAccounts(String keyword);

  Result<List<ResponsePointTransactionVO>> adminTransactions(Integer userId, String type);

  Result<String> adjust(RequestPointAdjustmentDTO request);

  Result<ResponsePointRuleSettingsVO> adminRuleSettings();

  Result<String> updateRuleSettings(RequestPointRuleSettingsDTO request);

  Result<List<ResponsePointRedeemItemVO>> adminItems(Boolean includeInactive);

  Result<Integer> createItem(RequestPointRedeemItemDTO request);

  Result<String> updateItem(Integer itemId, RequestPointRedeemItemDTO request);

  Result<String> deleteItem(Integer itemId);

  Result<List<ResponsePointRedemptionVO>> adminRedemptions(String status);

  Result<String> updateRedemptionStatus(Integer redemptionId, RequestPointRedemptionStatusDTO request);

  void grantCheckInPoints(Integer userId, CheckInSession session, ClubActivity activity, Integer operatorId);

  void revokeCheckInPoints(Integer userId, CheckInSession session, ClubActivity activity, Integer operatorId);

  void applyCheckInPenalty(
      Integer userId, CheckInSession session, String penaltyType, Long points, Integer operatorId);

  void revokeCheckInPenalty(
      Integer userId, CheckInSession session, String penaltyType, Integer operatorId);

  void grantDailyLoginPoints(Integer userId);

  void grantDailyLoginPointsPublic(Integer userId,String applicationName);

  void grantBlogPublishPoints(Integer userId, Integer articleId, String articleTitle, Integer operatorId);
}
