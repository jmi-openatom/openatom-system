package edu.jmi.openatom.server.openatomsystem.dto.request;

import edu.jmi.openatom.server.openatomsystem.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateUserStatusDTO {
  private UserStatus status;
}
