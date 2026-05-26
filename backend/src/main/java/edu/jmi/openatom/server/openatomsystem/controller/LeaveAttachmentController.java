package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.entity.LeaveApplication;
import edu.jmi.openatom.server.openatomsystem.mapper.LeaveApplicationMapper;
import edu.jmi.openatom.server.openatomsystem.service.impl.LeaveAttachmentStorageServiceImpl;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 受保护的请假附件读取接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/leave-attachments")
public class LeaveAttachmentController {
  private final LeaveAttachmentStorageServiceImpl leaveAttachmentStorageService;
  private final LeaveApplicationMapper leaveApplicationMapper;

  @GetMapping("/{fileName:.+}")
  public ResponseEntity<?> image(@PathVariable String fileName) throws IOException {
    LeaveApplication application = leaveApplicationMapper.selectByAttachmentFileName(fileName);
    if (application == null || !canView(application)) {
      return ResponseEntity.status(403).build();
    }
    return leaveAttachmentStorageService
        .load(fileName)
        .<ResponseEntity<?>>map(
            image ->
                ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(image.getMediaType())
                    .body(image.getResource()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private boolean canView(LeaveApplication application) {
    if (!StpUtil.isLogin()) return false;
    Integer currentUserId = StpUtil.getLoginIdAsInt();
    return currentUserId.equals(application.getUserId())
        || StpUtil.hasPermission("leave-application:detail")
        || StpUtil.hasPermission("leave-application:list")
        || StpUtil.hasPermission("leave-application:review");
  }
}
