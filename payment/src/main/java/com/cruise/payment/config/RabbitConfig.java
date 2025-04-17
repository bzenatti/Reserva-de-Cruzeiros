package com.cruise.payment.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange paymentApproved() {
        return new DirectExchange("payment-approved");
    }

    @Bean
    public DirectExchange paymentRejected() {
        return new DirectExchange("payment-rejected");
    }
}
