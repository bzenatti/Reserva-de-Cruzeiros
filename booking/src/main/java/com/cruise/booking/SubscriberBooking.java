package com.cruise.booking;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.cruise.booking.config.RabbitConfig;

@Component
public class SubscriberBooking {

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(String message) {
        informUser(message, true);
    }

    @RabbitListener(queues = RabbitConfig.DENIED_PAYMENT_QUEUE)
    public void listenToDeniedPayment(String message) {
        informUser(message, false);
    }

    @RabbitListener(queues = RabbitConfig.TICKET_QUEUE)
    public void listenToTicketGenerated(String message) {
        System.out.println("\nYour ticket has been generated! Ticket details:\n");
        System.out.println(message);
    }

    private void informUser(String message, boolean isApproved) {
        if (isApproved) {
            System.out.println("\nYour payment has been approved!\n");
        } else {
            System.out.println("\nYour payment was denied.\n");
        }
        // System.out.println(message);
    }
}