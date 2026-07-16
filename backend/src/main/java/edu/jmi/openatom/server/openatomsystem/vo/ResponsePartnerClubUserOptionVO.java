package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.Builder;
import lombok.Value;

/** 开源伙伴社长选择器中的站内用户低敏信息。 */
@Value
@Builder
public class ResponsePartnerClubUserOptionVO {
  Integer id;
  String userName;
  String realName;
  String studentId;
  String avatar;
}
