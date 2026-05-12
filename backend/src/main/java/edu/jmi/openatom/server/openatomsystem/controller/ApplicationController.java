package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseApplicationVO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.entity.MembershipApplication;
import edu.jmi.openatom.server.openatomsystem.service.ApplicationService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成员申请表控制器
 *
 * <p>提供成员申请的增删改查, 提交与撤回等操作
 */
@RestController
@RequiredArgsConstructor
public class ApplicationController {
  private final ApplicationService applicationService;

  /**
   * 分页查询申请表列表
   *
   * @param campaignId 招新活动ID
   * @param clubId 社团ID
   * @param status 申请状态
   * @param departmentId 部门ID
   * @param keyword 关键词
   * @param page 页码
   * @param pageSize 每页大小
   * @return 分页申请表数据
   */
  @GetMapping("/applications")
  @SaCheckPermission("application:list")
  public Result<PageDataVO<ResponseApplicationVO>> list(
      @RequestParam(required = false) Integer campaignId,
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") Long page,
      @RequestParam(defaultValue = "10") Long pageSize) {
    return applicationService.list(
        campaignId, clubId, status, departmentId, keyword, page, pageSize);
  }

  /**
   * 创建申请表
   *
   * @param request 创建申请请求
   * @return 申请表ID
   */
  @PostMapping("/applications")
  public Result<Integer> create(@Valid @RequestBody RequestCreateApplicationDTO request) {
    return applicationService.create(request);
  }

  /**
   * 获取申请表详情
   *
   * @param applicationId 申请表ID
   * @return 申请表详情
   */
  @GetMapping("/applications/{applicationId}")
  @SaCheckPermission("application:detail")
  public Result<MembershipApplication> detail(@PathVariable Integer applicationId) {
    return applicationService.detail(applicationId);
  }

  /**
   * 更新申请表
   *
   * @param applicationId 申请表ID
   * @param request 更新申请请求
   * @return 操作结果
   */
  @PatchMapping("/applications/{applicationId}")
  @SaCheckPermission("application:update")
  public Result<String> update(
      @PathVariable Integer applicationId,
      @Valid @RequestBody RequestUpdateApplicationDTO request) {
    return applicationService.update(applicationId, request);
  }

  /**
   * 提交申请表
   *
   * @param applicationId 申请表ID
   * @return 操作结果
   */
  @PostMapping("/applications/{applicationId}/submit")
  @SaCheckPermission("application:submit")
  public Result<String> submit(@PathVariable Integer applicationId) {
    return applicationService.submit(applicationId);
  }

  /**
   * 撤回申请表
   *
   * @param applicationId 申请表ID
   * @return 操作结果
   */
  @PostMapping("/applications/{applicationId}/withdraw")
  @SaCheckPermission("application:withdraw")
  public Result<String> withdraw(@PathVariable Integer applicationId) {
    return applicationService.withdraw(applicationId);
  }

  /**
   * 导出入会申请数据为Excel
   *
   * @param campaignId 招新活动ID
   * @param clubId 社团ID
   * @param status 申请状态
   * @param departmentId 部门ID
   * @param keyword 关键词
   * @return Excel文件响应
   */
  @GetMapping("/applications/export")
  @SaCheckPermission("application:list")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) Integer campaignId,
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) String keyword) {
    byte[] bytes = applicationService.exportExcel(campaignId, clubId, status, departmentId, keyword);
    String fileName =
        URLEncoder.encode("applications.xlsx", StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }
}
