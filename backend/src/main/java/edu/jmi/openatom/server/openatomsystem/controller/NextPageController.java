package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestNextPageJoinDTO;
import edu.jmi.openatom.server.openatomsystem.entity.NextPageJoin;
import edu.jmi.openatom.server.openatomsystem.mapper.NextPageJoinMapper;
import edu.jmi.openatom.server.openatomsystem.service.NextPageStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Next 页面统计接口
 *
 * <p>提供 openatom-system-next 页面的访问量、点赞数统计及加入开发申请
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/next-page")
public class NextPageController {

  private final NextPageStatsService nextPageStatsService;
  private final NextPageJoinMapper nextPageJoinMapper;

  /**
   * 获取页面统计数据（每次访问自动递增访问量）
   */
  @GetMapping("/stats")
  public Result<Map<String, Object>> getStats() {
    return nextPageStatsService.getStatsAndIncrementView();
  }

  /**
   * 点赞
   */
  @PostMapping("/like")
  public Result<Map<String, Object>> like() {
    return nextPageStatsService.incrementLike();
  }

  /**
   * 提交加入开发申请
   */
  @PostMapping("/join")
  public Result<Void> join(@RequestBody RequestNextPageJoinDTO dto) {
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Result.error("请填写姓名");
    }
    if (dto.getContact() == null || dto.getContact().isBlank()) {
      return Result.error("请填写联系方式");
    }
    if (dto.getDirection() == null || dto.getDirection().isBlank()) {
      return Result.error("请选择感兴趣的方向");
    }

    NextPageJoin join = NextPageJoin.builder()
        .name(dto.getName().trim())
        .contact(dto.getContact().trim())
        .direction(dto.getDirection().trim())
        .skills(dto.getSkills() != null ? dto.getSkills().trim() : "")
        .message(dto.getMessage() != null ? dto.getMessage().trim() : "")
        .build();
    nextPageJoinMapper.insert(join);

    return Result.success(null, "申请已提交，我们会尽快联系你！");
  }
}
