package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestGithubCallbackDTO {
  @NotBlank private String code;
  @NotBlank private String state;
}
