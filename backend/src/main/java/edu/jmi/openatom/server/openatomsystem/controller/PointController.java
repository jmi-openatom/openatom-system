package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointAdjustmentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedeemItemDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPointRedemptionStatusDTO;
import edu.jmi.openatom.server.openatomsystem.service.PointService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointAccountVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedeemItemVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointRedemptionVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointSummaryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePointTransactionVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PointController {
  private final PointService pointService;

  @GetMapping("/site/points/leaderboard")
  public Result<List<ResponsePointAccountVO>> leaderboard(
      @RequestParam(required = false) Integer limit) {
    return pointService.leaderboard(limit);
  }

  @GetMapping("/site/points/me")
  public Result<ResponsePointSummaryVO> mySummary() {
    return pointService.mySummary();
  }

  @GetMapping("/site/points/items")
  public Result<List<ResponsePointRedeemItemVO>> siteItems() {
    return pointService.siteItems();
  }

  @PostMapping("/site/points/items/{itemId}/redeem")
  public Result<Integer> redeem(
      @PathVariable Integer itemId, @RequestBody(required = false) RequestPointRedemptionDTO request) {
    return pointService.redeem(itemId, request);
  }

  @GetMapping("/site/points/redemptions")
  public Result<List<ResponsePointRedemptionVO>> myRedemptions() {
    return pointService.myRedemptions();
  }

  @GetMapping("/points/admin/accounts")
  @SaCheckPermission("point:account:list")
  public Result<List<ResponsePointAccountVO>> adminAccounts(
      @RequestParam(required = false) String keyword) {
    return pointService.adminAccounts(keyword);
  }

  @GetMapping("/points/admin/transactions")
  @SaCheckPermission("point:transaction:list")
  public Result<List<ResponsePointTransactionVO>> adminTransactions(
      @RequestParam(required = false) Integer userId,
      @RequestParam(required = false) String type) {
    return pointService.adminTransactions(userId, type);
  }

  @PostMapping("/points/admin/adjustments")
  @SaCheckPermission("point:adjust")
  public Result<String> adjust(@Valid @RequestBody RequestPointAdjustmentDTO request) {
    return pointService.adjust(request);
  }

  @GetMapping("/points/admin/items")
  @SaCheckPermission("point:item:list")
  public Result<List<ResponsePointRedeemItemVO>> adminItems(
      @RequestParam(required = false) Boolean includeInactive) {
    return pointService.adminItems(includeInactive);
  }

  @PostMapping("/points/admin/items")
  @SaCheckPermission("point:item:manage")
  public Result<Integer> createItem(@Valid @RequestBody RequestPointRedeemItemDTO request) {
    return pointService.createItem(request);
  }

  @PatchMapping("/points/admin/items/{itemId}")
  @SaCheckPermission("point:item:manage")
  public Result<String> updateItem(
      @PathVariable Integer itemId, @Valid @RequestBody RequestPointRedeemItemDTO request) {
    return pointService.updateItem(itemId, request);
  }

  @DeleteMapping("/points/admin/items/{itemId}")
  @SaCheckPermission("point:item:manage")
  public Result<String> deleteItem(@PathVariable Integer itemId) {
    return pointService.deleteItem(itemId);
  }

  @GetMapping("/points/admin/redemptions")
  @SaCheckPermission("point:redemption:list")
  public Result<List<ResponsePointRedemptionVO>> adminRedemptions(
      @RequestParam(required = false) String status) {
    return pointService.adminRedemptions(status);
  }

  @PatchMapping("/points/admin/redemptions/{redemptionId}/status")
  @SaCheckPermission("point:redemption:manage")
  public Result<String> updateRedemptionStatus(
      @PathVariable Integer redemptionId,
      @Valid @RequestBody RequestPointRedemptionStatusDTO request) {
    return pointService.updateRedemptionStatus(redemptionId, request);
  }
}
