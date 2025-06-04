package com.cruise.booking;

import com.cruise.booking.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublisherBooking {

    private final RabbitTemplate rabbitTemplate;

    public PublisherBooking(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBookingCreated(String itineraryMessage) {
        String routingKey = RabbitConfig.BOOKING_CREATED;

        rabbitTemplate.convertAndSend(
            RabbitConfig.BOOKING_CREATED,
            routingKey,
            itineraryMessage
        );

        // System.out.printf("Sent booking message to %s at %s%n",
                        //   routingKey, new java.util.Date());

    }
    public void sendBookingDeleted(String itineraryMessage) {
        String routingKey = RabbitConfig.BOOKING_DELETED;

        rabbitTemplate.convertAndSend(
            RabbitConfig.BOOKING_DELETED,
            routingKey,
            itineraryMessage
        );

        // System.out.printf("Sent booking message to %s at %s%n",
                        //   routingKey, new java.util.Date());
    }
}
