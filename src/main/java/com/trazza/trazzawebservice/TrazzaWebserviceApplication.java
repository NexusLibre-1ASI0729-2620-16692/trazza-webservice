package com.trazza.trazzawebservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrazzaWebserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrazzaWebserviceApplication.class, args);
    }

}
