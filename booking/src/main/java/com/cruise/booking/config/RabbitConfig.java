package com.cruise.booking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitConfig {

    public static final String BOOKING_CREATED = "booking-created"; 
    public static final String BOOKING_DELETED = "booking-deleted"; 
    public static final String APPROVED_PAYMENT = "approved-payment";
    public static final String DENIED_PAYMENT = "denied-payment";
    public static final String TICKET_GENERATED = "ticket-generated";
    
    public static final String APPROVED_PAYMENT_QUEUE = "approved-payment.queue.bookingMS";
    public static final String DENIED_PAYMENT_QUEUE = "denied-payment.queue.bookingMS";
    public static final String TICKET_QUEUE = "ticket.queue.bookingMS";

    @Bean
    public DirectExchange bookingCreatedExchange() {
        return new DirectExchange(BOOKING_CREATED);
    }
    
    @Bean
    public DirectExchange bookingDeletedExchange() {
        return new DirectExchange(BOOKING_DELETED);
    }

    @Bean
    public DirectExchange approvedPaymentExchange() {
        return new DirectExchange(APPROVED_PAYMENT);
    }

    @Bean
    public DirectExchange deniedPaymentExchange() {
        return new DirectExchange(DENIED_PAYMENT);
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
    public Queue deniedPaymentQueue() {
        return new Queue(DENIED_PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue ticketQueue() {
        return new Queue(TICKET_QUEUE, true);
    }

    @Bean
    public Binding approvedPaymentBinding(DirectExchange approvedPaymentExchange, @Qualifier("approvedPaymentQueue") Queue approvedPaymentQueue) {
        return BindingBuilder.bind(approvedPaymentQueue)
                .to(approvedPaymentExchange)
                .with("approved-payment");
    }

    @Bean
    public Binding deniedPaymentBinding(DirectExchange deniedPaymentExchange, @Qualifier("deniedPaymentQueue") Queue deniedPaymentQueue) {
        return BindingBuilder.bind(deniedPaymentQueue)
                .to(deniedPaymentExchange)
                .with("denied-payment");
    }

    @Bean
    public Binding ticketBinding(DirectExchange ticketExchange, @Qualifier("ticketQueue") Queue ticketQueue) {
        return BindingBuilder.bind(ticketQueue)
                .to(ticketExchange)
                .with("ticket.generated");
    }
}