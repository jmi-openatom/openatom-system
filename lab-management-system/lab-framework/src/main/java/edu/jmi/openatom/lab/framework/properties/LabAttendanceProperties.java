package edu.jmi.openatom.lab.framework.properties;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lab.attendance")
public class LabAttendanceProperties {
  private LocalTime standardTime = LocalTime.of(9, 0);
  private LocalTime lateCutoffTime = LocalTime.of(9, 30);
  private int onsiteScoreAward = 2;
  private int autoScoreAward = 3;
  private int latePenalty = -5;
  private int absentPenalty = -10;
  private List<String> allowedCidrs = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));
}
