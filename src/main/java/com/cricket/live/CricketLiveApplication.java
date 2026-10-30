package com.cricket.live;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CricketLiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(CricketLiveApplication.class, args);
    }
}
