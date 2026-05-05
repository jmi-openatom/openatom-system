package edu.jmi.openatom.server.openatomsystem.dto.response;

import edu.jmi.openatom.server.openatomsystem.entity.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseLoginDTO {
	private String accessToken;
	private String refreshToken;
	private Integer expiresIn;
	private User user;
	private List<String> roles;
	private List<String> permissions;
}
