package com.landfun.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class LandfunBootApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LandfunBootApplication.class);
        // Optimize startup for analysis if needed, or just standard startup
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
