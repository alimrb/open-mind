package com.openmind.hagap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HagapApplication {

    public static void main(String[] args) {
        SpringApplication.run(HagapApplication.class, args);
    }
}
