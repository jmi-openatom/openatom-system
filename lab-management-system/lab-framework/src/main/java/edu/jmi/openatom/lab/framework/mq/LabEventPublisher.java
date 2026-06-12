package edu.jmi.openatom.lab.framework.mq;

import edu.jmi.openatom.lab.framework.properties.LabMqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabEventPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final LabMqProperties properties;

  public void publishClubScore(Object event) {
    rabbitTemplate.convertAndSend(properties.getExchange(), properties.getClubScoreRoutingKey(), event);
  }
}
