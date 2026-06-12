package edu.jmi.openatom.lab.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import edu.jmi.openatom.lab.framework.properties.LabAiProperties;
import edu.jmi.openatom.lab.framework.properties.LabAttendanceProperties;
import edu.jmi.openatom.lab.framework.properties.LabAuthProperties;
import edu.jmi.openatom.lab.framework.properties.LabMqProperties;
import edu.jmi.openatom.lab.framework.properties.LabSandboxProperties;
import org.springframework.amqp.core.TopicExchange;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
@EnableScheduling
@MapperScan("edu.jmi.openatom.lab.**.mapper")
@EnableConfigurationProperties({
  LabAuthProperties.class,
  LabMqProperties.class,
  LabAttendanceProperties.class,
  LabAiProperties.class,
  LabSandboxProperties.class
})
public class LabFrameworkConfig {
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  public TopicExchange labEventExchange(LabMqProperties properties) {
    return new TopicExchange(properties.getExchange(), true, false);
  }

  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
  }
}
