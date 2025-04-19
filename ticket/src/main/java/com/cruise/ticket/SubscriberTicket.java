package com.cruise.ticket;

import com.cruise.ticket.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriberTicket {

    private final PublisherTicket publisherTicket;

    public SubscriberTicket(PublisherTicket publisherTicket) {
        this.publisherTicket = publisherTicket;
    }

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(String message) {
        System.out.println("\nReceived approved payment: " + message);

        String ticketDetails = generateTicket(message);
        publisherTicket.publishTicketGenerated(ticketDetails);
    }

    private String generateTicket(String message) {
        System.out.println("\nGenerating ticket for booking: " + message);
        return message;
    }
}