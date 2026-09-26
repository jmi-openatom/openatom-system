package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestClubExpenseDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestVoidClubExpenseDTO;
import edu.jmi.openatom.server.openatomsystem.service.impl.ClubExpenseService;
import edu.jmi.openatom.server.openatomsystem.service.impl.ClubExpenseService.Filter;
import edu.jmi.openatom.server.openatomsystem.vo.ClubExpenseVO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/club-expenses")
public class ClubExpenseController {
  private final ClubExpenseService service;

  @GetMapping("/clubs")
  @SaCheckPermission("expense:list")
  public Result<List<ClubExpenseVO.ClubOption>> clubs() {
    return Result.success(service.clubs());
  }

  @GetMapping("/activities")
  @SaCheckPermission("expense:list")
  public Result<List<ClubExpenseVO.ActivityOption>> activities(@RequestParam Integer clubId) {
    return Result.success(service.activities(clubId));
  }

  @GetMapping
  @SaCheckPermission("expense:list")
  public Result<PageDataVO<ClubExpenseVO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer activityId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return Result.success(service.list(filter(clubId, startDate, endDate, category, activityId, status, keyword), page, pageSize));
  }

  @GetMapping("/summary")
  @SaCheckPermission("expense:list")
  public Result<ClubExpenseVO.Summary> summary(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer activityId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return Result.success(service.summary(filter(clubId, startDate, endDate, category, activityId, status, keyword)));
  }

  @GetMapping("/export")
  @SaCheckPermission("expense:export")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Integer activityId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    byte[] csv = service.exportCsv(filter(clubId, startDate, endDate, category, activityId, status, keyword));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("社团支出记录.csv", StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(csv);
  }

  @GetMapping("/{expenseId}")
  @SaCheckPermission("expense:detail")
  public Result<ClubExpenseVO> detail(@PathVariable Long expenseId) {
    return Result.success(service.detail(expenseId));
  }

  @GetMapping("/{expenseId}/history")
  @SaCheckPermission("expense:detail")
  public Result<List<ClubExpenseVO.History>> history(@PathVariable Long expenseId) {
    return Result.success(service.history(expenseId));
  }

  @PostMapping
  @SaCheckPermission("expense:create")
  public Result<ClubExpenseVO> create(@Valid @RequestBody RequestClubExpenseDTO request) {
    return Result.success(service.create(request));
  }

  @PatchMapping("/{expenseId}")
  @SaCheckPermission("expense:update")
  public Result<ClubExpenseVO> update(@PathVariable Long expenseId,
      @Valid @RequestBody RequestClubExpenseDTO request) {
    return Result.success(service.update(expenseId, request));
  }

  @PostMapping("/{expenseId}/void")
  @SaCheckPermission("expense:void")
  public Result<ClubExpenseVO> voidExpense(@PathVariable Long expenseId,
      @Valid @RequestBody RequestVoidClubExpenseDTO request) {
    return Result.success(service.voidExpense(expenseId, request.reason(), request.version()));
  }

  @PostMapping(value = "/{expenseId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SaCheckPermission("expense:attachment")
  public Result<ClubExpenseVO.Attachment> upload(@PathVariable Long expenseId,
      @RequestParam("file") MultipartFile file) {
    return Result.success(service.upload(expenseId, file));
  }

  @DeleteMapping("/{expenseId}/attachments/{attachmentId}")
  @SaCheckPermission("expense:attachment")
  public Result<String> removeAttachment(@PathVariable Long expenseId, @PathVariable Long attachmentId) {
    service.deleteAttachment(expenseId, attachmentId);
    return Result.success("凭证已移除");
  }

  @GetMapping("/{expenseId}/attachments/{attachmentId}")
  @SaCheckPermission("expense:detail")
  public ResponseEntity<Resource> download(@PathVariable Long expenseId, @PathVariable Long attachmentId) {
    ClubExpenseService.Download file = service.download(expenseId, attachmentId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.originalName(), StandardCharsets.UTF_8).build().toString())
        .contentType(MediaType.parseMediaType(file.mimeType()))
        .contentLength(file.size())
        .body(file.resource());
  }

  private Filter filter(Integer clubId, LocalDate startDate, LocalDate endDate,
      String category, Integer activityId, String status, String keyword) {
    return new Filter(clubId, startDate, endDate, category, activityId, status, keyword);
  }
}
