package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSubmitVoteDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateVoteDTO;
import edu.jmi.openatom.server.openatomsystem.service.VoteService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteDetailVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteRecordVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseVoteVO;
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

/** 投票控制器 */
@RestController
@RequiredArgsConstructor
public class VoteController {
  private final VoteService voteService;

  @GetMapping("/votes")
  @SaCheckPermission("vote:list")
  public Result<List<ResponseVoteVO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status) {
    return voteService.list(clubId, status);
  }

  @GetMapping("/votes/{voteId}")
  @SaCheckPermission("vote:detail")
  public Result<ResponseVoteDetailVO> detail(@PathVariable Integer voteId) {
    return voteService.detail(voteId);
  }

  @PostMapping("/clubs/{clubId}/votes")
  @SaCheckPermission("vote:create")
  public Result<String> create(
      @PathVariable Integer clubId, @Valid @RequestBody RequestCreateVoteDTO request) {
    return voteService.create(clubId, request);
  }

  @PatchMapping("/votes/{voteId}")
  @SaCheckPermission("vote:update")
  public Result<String> update(
      @PathVariable Integer voteId, @Valid @RequestBody RequestUpdateVoteDTO request) {
    return voteService.update(voteId, request);
  }

  @PostMapping("/votes/{voteId}/publish")
  @SaCheckPermission("vote:update")
  public Result<String> publish(@PathVariable Integer voteId) {
    return voteService.publish(voteId);
  }

  @PostMapping("/votes/{voteId}/close")
  @SaCheckPermission("vote:update")
  public Result<String> close(@PathVariable Integer voteId) {
    return voteService.close(voteId);
  }

  @PostMapping("/votes/{voteId}/reset")
  @SaCheckPermission("vote:manage-records")
  public Result<String> reset(@PathVariable Integer voteId) {
    return voteService.reset(voteId);
  }

  @GetMapping("/site/votes")
  public Result<List<ResponseVoteVO>> siteList(@RequestParam(required = false) Integer clubId) {
    return voteService.siteList(clubId);
  }

  @GetMapping("/site/votes/{voteId}")
  public Result<ResponseVoteDetailVO> siteDetail(@PathVariable Integer voteId) {
    return voteService.siteDetail(voteId);
  }

  @PostMapping("/site/votes/{voteId}/records")
  public Result<ResponseVoteRecordVO> submit(
      @PathVariable Integer voteId, @Valid @RequestBody RequestSubmitVoteDTO request) {
    return voteService.submit(voteId, request);
  }
}
