package com.bankguard.auditsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.bankguard.auditsearch", "com.bankguard.common"})
public class AuditSearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditSearchServiceApplication.class, args);
    }
}
