package com.bankguard.transaction.config;

import com.bankguard.common.event.EventEnvelopeFactory;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionServiceConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    EventEnvelopeFactory eventEnvelopeFactory() {
        return EventEnvelopeFactory.system();
    }
}
