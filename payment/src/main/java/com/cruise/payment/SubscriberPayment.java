package com.cruise.payment;

import com.cruise.payment.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class SubscriberPayment {

    private final PaymentPublisher paymentPublisher;

    public SubscriberPayment(PaymentPublisher paymentPublisher) {
        this.paymentPublisher = paymentPublisher;
    }

    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE)
    public void receiveBookingMessage(String message) {
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        processPayment(message);

        System.out.printf("\nDo you want to approve this payment?\nType 1 for yes and 2 for no: ");
        Scanner scanner = new Scanner(System.in);
        int decision = scanner.nextInt();
        scanner.close();
        try {
            if (decision == 1) {
                paymentPublisher.publishApprovedPayment(message);
            } else if (decision == 2) {
                paymentPublisher.publishDeniedPayment(message);
            } else {
                System.out.println("Invalid input. Payment decision not recorded.");
            }
        } catch (Exception e) {
            System.err.println("Error processing payment decision: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processPayment(String message) {
        String[] details = message.split(",");
        String username = details[0];
        String destination = details[1];
        String shipName = details[2];
        String departureDate = details[5];
        int passengers = Integer.parseInt(details[6]);
        double pricePerPerson = Double.parseDouble(details[10]);
        double totalPrice = passengers * pricePerPerson;
        System.out.printf("\n\nPayment for %s (%s cruise to %s on %s). Total amount: $%.2f%n",
                            username, shipName, destination, departureDate, totalPrice);
    }
}