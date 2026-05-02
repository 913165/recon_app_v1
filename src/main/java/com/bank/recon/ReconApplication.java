package com.bank.recon;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReconApplication {

    public static void main(String[] args) {
        // PostgreSQL rejects legacy ID "Asia/Calcutta" from the JDBC startup packet; use IANA "Asia/Kolkata".
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Kolkata")));
        SpringApplication.run(ReconApplication.class, args);
    }
}
