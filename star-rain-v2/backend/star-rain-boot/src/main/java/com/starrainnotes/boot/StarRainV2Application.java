package com.starrainnotes.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.starrainnotes")
public class StarRainV2Application {

    public static void main(String[] args) {
        SpringApplication.run(StarRainV2Application.class, args);
    }
}

