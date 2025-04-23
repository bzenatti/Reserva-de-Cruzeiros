package com.cruise.booking;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;


import com.cruise.booking.config.RabbitConfig;

@Component
public class SubscriberBooking {

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(Message amqpMessage) {
        try {
            // Extract the payload (message) and signature 
            String message = new String(amqpMessage.getBody());
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            // Verify the message
            if (verifyMessage(message, signature)) {
                System.out.println("\nValid approved payment received:\n" + message);
                informUser(message, true);
            } else {
                System.out.println("\nInvalid signature detected. Message discarded.");
            }
        } catch (Exception e) {
            System.err.println("Error while processing approved payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.DENIED_PAYMENT_QUEUE)
    public void listenToDeniedPayment(String message) {
        informUser(message, false);
    }

    @RabbitListener(queues = RabbitConfig.TICKET_QUEUE)
    public void listenToTicketGenerated(String message) {
        System.out.println("\nYour ticket has been generated!\n Ticket details:\n");
        displayTicketDetails(message);
    }

    private void displayTicketDetails(String message) {
        String[] fields = message.split(",");

        String username = fields[0];
        String destination = fields[1];
        String shipName = fields[2];
        String embarkationPort = fields[3];
        String disembarkationPort = fields[4];
        String departureDate = fields[5];
        int passengers = Integer.parseInt(fields[6]);
        int cabins = Integer.parseInt(fields[7]);
        String visitedPlaces = fields[8].replace(";", ", "); 
        int nights = Integer.parseInt(fields[9]);
        double pricePerPerson = Double.parseDouble(fields[10]);

        System.out.printf("Passenger Name: %s%n", username);
        System.out.printf("Destination: %s%n", destination);
        System.out.printf("Ship Name: %s%n", shipName);
        System.out.printf("Embarkation Port: %s%n", embarkationPort);
        System.out.printf("Disembarkation Port: %s%n", disembarkationPort);
        System.out.printf("Departure Date: %s%n", departureDate);
        System.out.printf("Number of Passengers: %d%n", passengers);
        System.out.printf("Number of Cabins: %d%n", cabins);
        System.out.printf("Visited Places: %s%n", visitedPlaces);
        System.out.printf("Number of Nights: %d%n", nights);
        System.out.printf("Price Per Person: $%.2f%n", pricePerPerson);
        System.out.printf("Total Price: $%.2f%n", passengers * pricePerPerson);
    }

    private void informUser(String message, boolean isApproved) {
        if (isApproved) {
            System.out.println("\nYour payment has been approved!\n");
        } else {
            System.out.println("\nYour payment was denied.\n");
        }
        // System.out.println(message);
    }
    private boolean verifyMessage(String message, String signature) throws Exception {
        byte[] publicKeyBytes = Files.readAllBytes(Paths.get("payment-public.key"));
        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);

        
        sig.update(message.getBytes());
        byte[] signedBytes = Base64.getDecoder().decode(signature);

        return sig.verify(signedBytes);
    }
}