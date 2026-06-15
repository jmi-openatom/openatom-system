package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.ClubAccessService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubAccessVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CurrentUserController {
  private final ClubAccessService clubAccessService;

  @GetMapping("/me/clubs")
  @SaCheckPermission("auth:me")
  public Result<List<ResponseClubAccessVO>> myClubs() {
    return clubAccessService.myClubs();
  }
}
