package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** 开源伙伴公开响应，避免暴露草稿状态等内部字段。 */
@Value
@Builder
public class ResponsePartnerClubVO {
  Integer id;
  String name;
  String logoUrl;
  String description;
  String websiteUrl;
  String organization;
  String category;
  List<String> tags;
  Integer sortOrder;
  Boolean featured;
}
