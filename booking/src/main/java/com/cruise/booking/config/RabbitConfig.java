package com.cruise.booking.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String BOOKING_CREATED = "booking-created";

    @Bean
    public DirectExchange bookingCreated() {
        return new DirectExchange(BOOKING_CREATED);
    }
}
