package edu.jmi.openatom.server.openatomsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("edu.jmi.openatom.server.openatomsystem.mapper")
public class OpenAtomSystemApplication {

  public static void main(String[] args) {
    SpringApplication.run(OpenAtomSystemApplication.class, args);
  }
}
