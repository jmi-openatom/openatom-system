package edu.jmi.openatom.lab.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lab.mq")
public class LabMqProperties {
  private String exchange = "openatom.lab.events";
  private String clubScoreRoutingKey = "club.score.add";
}
