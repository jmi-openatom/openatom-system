package edu.jmi.openatom.quest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("edu.jmi.openatom.quest.mapper")
@SpringBootApplication
@EnableScheduling
public class OpenAtomQuestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAtomQuestApplication.class, args);
    }
}
