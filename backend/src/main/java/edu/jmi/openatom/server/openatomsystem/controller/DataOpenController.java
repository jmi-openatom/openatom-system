package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewDataOpenApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.service.DataOpenApplicationService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseDataOpenApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据开放平台后台管理控制器
 *
 * <p>提供开放平台申请列表和审核能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/data-open")
public class DataOpenController {
  private final DataOpenApplicationService dataOpenApplicationService;

  @GetMapping("/admin/applications")
  @SaCheckPermission("data-open:list")
  public Result<PageDataVO<ResponseDataOpenApplicationVO>> adminApplications(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return dataOpenApplicationService.adminList(keyword, status, page, pageSize);
  }

  @PostMapping("/admin/applications/{applicationId}/review")
  @SaCheckPermission("data-open:review")
  public Result<ResponseDataOpenApplicationVO> reviewApplication(
      @PathVariable Integer applicationId,
      @Valid @RequestBody RequestReviewDataOpenApplicationDTO request) {
    return dataOpenApplicationService.review(applicationId, request, StpUtil.getLoginIdAsInt());
  }


}
