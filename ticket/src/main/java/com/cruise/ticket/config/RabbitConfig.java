package com.cruise.ticket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String APPROVED_PAYMENT = "approved-payment";
    public static final String TICKET_GENERATED = "ticket-generated";
    public static final String APPROVED_PAYMENT_QUEUE = "approved-payment.queue.ticketMS";
    public static final String TICKET_QUEUE = "ticket.queue";

    @Bean
    public DirectExchange approvedPaymentExchange() {
        return new DirectExchange(APPROVED_PAYMENT);
    }

    @Bean
    public DirectExchange ticketExchange() {
        return new DirectExchange(TICKET_GENERATED);
    }

    @Bean
    public Queue approvedPaymentQueue() {
        return new Queue(APPROVED_PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue ticketQueue() {
        return new Queue(TICKET_QUEUE, true);
    }

    @Bean
    public Binding approvedPaymentBinding(DirectExchange approvedPaymentExchange, Queue approvedPaymentQueue) {
        return BindingBuilder.bind(approvedPaymentQueue)
                .to(approvedPaymentExchange)
                .with("approved-payment");
    }

    @Bean
    public Binding ticketBinding(DirectExchange ticketExchange, Queue ticketQueue) {
        return BindingBuilder.bind(ticketQueue)
                .to(ticketExchange)
                .with("ticket.generated");
    }
}