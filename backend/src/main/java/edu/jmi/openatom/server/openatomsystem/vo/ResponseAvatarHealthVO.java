package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.entity.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAvatarHealthVO {
  private Integer totalManaged;
  private Integer invalidCount;
  private List<User> invalidUsers;
}
