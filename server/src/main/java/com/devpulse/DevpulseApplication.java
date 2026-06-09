package com.devpulse;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class DevpulseApplication {
    public static void main(String[] args) {
        TimeZone currentZone = TimeZone.getDefault();

        if ("Asia/Calcutta".equals(currentZone.getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }

        SpringApplication.run(DevpulseApplication.class, args);
    }
}
