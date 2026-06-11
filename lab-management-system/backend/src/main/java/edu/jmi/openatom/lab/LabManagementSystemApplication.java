package edu.jmi.openatom.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LabManagementSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(LabManagementSystemApplication.class, args);
  }
}
