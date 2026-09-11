package com.dreamuniverse_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DreamuniverseServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DreamuniverseServerApplication.class, args);
    }

}
