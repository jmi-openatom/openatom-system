package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.BotUserLookupService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBotUserLookupVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 机器人公开查人接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bot/users")
public class BotUserLookupController {
  private final BotUserLookupService botUserLookupService;

  @GetMapping("/lookup")
  public Result<List<ResponseBotUserLookupVO>> lookup(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String qqOpenid,
      @RequestParam(defaultValue = "5") Integer limit) {
    return botUserLookupService.lookup(keyword, qqOpenid, limit);
  }
}
