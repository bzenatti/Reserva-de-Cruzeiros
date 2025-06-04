package com.cruise.payment.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String BOOKING_QUEUE = "payment.booking.queue";

    public static final String APPROVED_PAYMENT = "approved-payment";
    public static final String DENIED_PAYMENT = "denied-payment";

    @Bean
    public Queue paymentBookingQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    @Bean
    public DirectExchange paymentApprovedExchange() {
        return new DirectExchange(APPROVED_PAYMENT);
    }

    @Bean
    public DirectExchange paymentDeniedExchange() {
        return new DirectExchange(DENIED_PAYMENT);
    }

}