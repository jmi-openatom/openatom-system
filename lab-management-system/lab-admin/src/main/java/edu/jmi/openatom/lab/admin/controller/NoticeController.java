package edu.jmi.openatom.lab.admin.controller;

import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import edu.jmi.openatom.lab.framework.auth.LabSecurityContext;
import edu.jmi.openatom.lab.notice.service.NoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {
  private final NoticeService noticeService;

  @GetMapping
  public Result<List<LabDtos.NotificationView>> list() {
    return Result.success(noticeService.listForUser(LabSecurityContext.userId()));
  }

  @PatchMapping("/{id}/read")
  public Result<String> read(@PathVariable Long id) {
    noticeService.markRead(id, LabSecurityContext.userId());
    return Result.success("已读");
  }
}
