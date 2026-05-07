package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponsePositionDTO;
import edu.jmi.openatom.server.openatomsystem.service.PositionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PositionController {
  private final PositionService positionService;

  @GetMapping("/clubs/{clubId}/positions")
  @SaCheckPermission("position:list")
  public ApiResponse<List<ResponsePositionDTO>> getPositionsByClubId(@PathVariable Integer clubId) {
    return positionService.getPositionsByClubId(clubId);
  }

  @PostMapping("/clubs/{clubId}/positions")
  @SaCheckPermission("position:create")
  public ApiResponse<String> createPosition(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreatePositionDTO requestCreatePositionDTO) {
    return positionService.createPosition(clubId, requestCreatePositionDTO);
  }

  @GetMapping("/positions/{positionId}")
  @SaCheckPermission("position:detail")
  public ApiResponse<ResponsePositionDTO> getPositionById(@PathVariable Integer positionId) {
    return positionService.getPositionById(positionId);
  }

  @PatchMapping("/positions/{positionId}")
  @SaCheckPermission("position:update")
  public ApiResponse<String> updatePosition(
      @PathVariable Integer positionId,
      @Valid @RequestBody RequestUpdatePositionDTO requestUpdatePositionDTO) {
    return positionService.updatePosition(positionId, requestUpdatePositionDTO);
  }

  @DeleteMapping("/positions/{positionId}")
  @SaCheckPermission("position:delete")
  public ApiResponse<String> deletePosition(@PathVariable Integer positionId) {
    return positionService.deletePosition(positionId);
  }
}
