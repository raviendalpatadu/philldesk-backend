package com.philldesk.philldeskbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhilldeskBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhilldeskBackendApplication.class, args);
    }

}
