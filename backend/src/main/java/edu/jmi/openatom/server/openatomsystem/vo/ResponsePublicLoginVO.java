package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponsePublicLoginVO {
	private String accessToken;
	private Integer expiresIn;
	private User user;
	private Integer applicationId;
	private String applicationName;
}
