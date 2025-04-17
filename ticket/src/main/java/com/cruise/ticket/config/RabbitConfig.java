package com.cruise.ticket.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange ticketExchange() {
        return new DirectExchange("ticket-generated");
    }
}
