package com.starrainnotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Star Rain Notes V1 backend entry point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StarRainNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarRainNotesApplication.class, args);
    }
}
