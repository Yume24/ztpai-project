package com.rendezvous;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RendezvousApplication {

    public static void main(String[] args) {
        SpringApplication.run(RendezvousApplication.class, args);
    }
}
