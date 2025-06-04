package com.cruise.itineraries.config;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String BOOKING_CREATED = "booking-created"; 
    public static final String BOOKING_DELETED = "booking-deleted"; 

    public static final String BOOKING_CREATED_QUEUE = "itineraries.booking.created.queue";
    public static final String BOOKING_DELETED_QUEUE = "itineraries.booking.deleted.queue";

    @Bean
    public DirectExchange bookingCreatedExchange() {
        return new DirectExchange(BOOKING_CREATED);
    }

    @Bean
    public DirectExchange bookingDeletedExchange() {
        return new DirectExchange(BOOKING_DELETED);
    }

    @Bean
    public Queue bookingCreatedQueue() {
        return new Queue(BOOKING_CREATED_QUEUE, true);
    }

    @Bean
    public Queue bookingDeletedQueue() {
        return new Queue(BOOKING_DELETED_QUEUE, true);
    }

    @Bean
    public Binding bookingCreatedBinding(DirectExchange bookingCreatedExchange, @Qualifier("bookingCreatedQueue") Queue bookingCreatedQueue) {
        return BindingBuilder.bind(bookingCreatedQueue)
                .to(bookingCreatedExchange)
                .with("booking-created");
    }

    @Bean
    public Binding bookingDeletedBinding(DirectExchange bookingDeletedExchange, @Qualifier("bookingDeletedQueue") Queue bookingDeletedQueue) {
        return BindingBuilder.bind(bookingDeletedQueue)
                .to(bookingDeletedExchange)
                .with("booking-deleted");
    }
}
