package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestDrawLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateLotteryDTO;
import edu.jmi.openatom.server.openatomsystem.service.LotteryService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryScreenVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseLotteryWinnerVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 抽奖系统控制器 */
@RestController
@RequiredArgsConstructor
public class LotteryController {
  private final LotteryService lotteryService;

  @GetMapping("/lotteries")
  @SaCheckPermission("lottery:list")
  public Result<List<ResponseLotteryVO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status) {
    return lotteryService.list(clubId, status);
  }

  @GetMapping("/lotteries/{lotteryId}")
  @SaCheckPermission("lottery:detail")
  public Result<ResponseLotteryDetailVO> detail(@PathVariable Integer lotteryId) {
    return lotteryService.detail(lotteryId);
  }

  @PostMapping("/clubs/{clubId}/lotteries")
  @SaCheckPermission("lottery:create")
  public Result<String> create(
      @PathVariable Integer clubId, @Valid @RequestBody RequestCreateLotteryDTO request) {
    return lotteryService.create(clubId, request);
  }

  @PatchMapping("/lotteries/{lotteryId}")
  @SaCheckPermission("lottery:update")
  public Result<String> update(
      @PathVariable Integer lotteryId, @Valid @RequestBody RequestUpdateLotteryDTO request) {
    return lotteryService.update(lotteryId, request);
  }

  @PostMapping("/lotteries/{lotteryId}/publish")
  @SaCheckPermission("lottery:update")
  public Result<String> publish(@PathVariable Integer lotteryId) {
    return lotteryService.publish(lotteryId);
  }

  @PostMapping("/lotteries/{lotteryId}/close")
  @SaCheckPermission("lottery:update")
  public Result<String> close(@PathVariable Integer lotteryId) {
    return lotteryService.close(lotteryId);
  }

  @PostMapping("/lotteries/{lotteryId}/draw")
  @SaCheckPermission("lottery:draw")
  public Result<ResponseLotteryWinnerVO> draw(
      @PathVariable Integer lotteryId, @RequestBody(required = false) RequestDrawLotteryDTO request) {
    return lotteryService.draw(lotteryId, request);
  }

  @PostMapping("/lotteries/{lotteryId}/reset")
  @SaCheckPermission("lottery:draw")
  public Result<String> reset(@PathVariable Integer lotteryId) {
    return lotteryService.reset(lotteryId);
  }

  @GetMapping("/site/lotteries/{lotteryId}/screen")
  public Result<ResponseLotteryScreenVO> screen(@PathVariable Integer lotteryId) {
    return lotteryService.screen(lotteryId);
  }
}
