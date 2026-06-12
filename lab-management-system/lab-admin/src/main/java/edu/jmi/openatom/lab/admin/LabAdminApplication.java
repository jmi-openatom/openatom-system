package edu.jmi.openatom.lab.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "edu.jmi.openatom.lab")
public class LabAdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(LabAdminApplication.class, args);
  }
}
