package edu.jmi.openatom.server.openatomsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * <p>Spring Boot 应用入口, 配置 Mapper 扫描路径
 */
@SpringBootApplication
@MapperScan("edu.jmi.openatom.server.openatomsystem.mapper")
@EnableScheduling
public class OpenAtomSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(OpenAtomSystemApplication.class, args);
  }
}
