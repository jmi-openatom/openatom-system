package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewEvaluationTemplate;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseInterviewEvaluationTemplateVO {
  private Integer id;
  private Integer campaignId;
  private String name;
  private Integer version;
  private String status;
  private Map<String, Object> schema;

  public static ResponseInterviewEvaluationTemplateVO from(InterviewEvaluationTemplate template) {
    if (template == null) return null;
    return builder().id(template.getId()).campaignId(template.getCampaignId()).name(template.getName())
        .version(template.getVersion()).status(template.getStatus())
        .schema(Jsons.parseObject(template.getSchemaJson())).build();
  }
}
