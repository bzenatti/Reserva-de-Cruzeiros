package com.cruise.booking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.amqp.core.Message;

import com.cruise.booking.config.RabbitConfig;

@Component
public class SubscriberBooking {

    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(Message amqpMessage) {
        try {
            String message = new String(amqpMessage.getBody());
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            if (verifyMessage(message, signature)) {
                System.out.println("\nValid approved payment received:\n" + message);
                informUser(message, true);
                sendSseEvent("payment_approved", "Your payment has been approved! Details: " + message);
            } else {
                System.out.println("\nInvalid signature detected. Message discarded.");
                sendSseEvent("payment_error", "Invalid payment signature received. Message discarded.");
            }
        } catch (Exception e) {
            System.err.println("Error while processing approved payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.DENIED_PAYMENT_QUEUE)
    public void listenToDeniedPayment(Message amqpMessage) {
        try {
            String message = new String(amqpMessage.getBody());
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            if (verifyMessage(message, signature)) {
                System.out.println("\nValid denied payment received:\n" + message);
                informUser(message, false);
                sendSseEvent("payment_denied", "Your payment was denied. Details: " + message);
            } else {
                System.out.println("\nInvalid signature detected for denied payment. Message discarded.");
                sendSseEvent("payment_error", "Invalid payment signature received. Message discarded.");
            }
        } catch (Exception e) {
            System.err.println("Error while processing denied payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.TICKET_QUEUE)
    public void listenToTicketGenerated(String message) {
        System.out.println("\nYour ticket has been generated!\nTicket details:\n");
        displayTicketDetails(message);
        sendSseEvent("ticket_generated", "Your ticket has been generated! Details: " + message);
    }

    @RabbitListener(queues = RabbitConfig.PROMOTIONS_QUEUE_BOOKING)
    public void listenToPromotions(String promotionMessage) {
        System.out.println("Received promotion: " + promotionMessage);
        sendSseEvent("promotion", promotionMessage);
    }

    private void sendSseEvent(String eventName, String data) {
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                sseEmitters.remove(emitter);
            }
        }
    }

    public void addSseEmitter(SseEmitter emitter) {
        this.sseEmitters.add(emitter);
        emitter.onCompletion(() -> this.sseEmitters.remove(emitter));
        emitter.onTimeout(() -> this.sseEmitters.remove(emitter));
        emitter.onError(e -> this.sseEmitters.remove(emitter));
    }

    public void removeSseEmitter(SseEmitter emitter) {
        this.sseEmitters.remove(emitter);
    }

    private void displayTicketDetails(String message) {
        try {
            String[] fields = message.split(",");
            if (fields.length < 12) {
                System.err.println("Malformed ticket message received. Not enough fields.");
                return;
            }

            String reservationId = fields[0];
            String clientName = fields[1];
            String destination = fields[2];
            String shipName = fields[3];
            String embarkationPort = fields[4];
            String disembarkationPort = fields[5];
            String departureDate = fields[6];
            int passengers = Integer.parseInt(fields[7]);
            int cabins = Integer.parseInt(fields[8]);
            String visitedPlaces = fields[9].replace(";", ", ");
            int nights = Integer.parseInt(fields[10]);
            double pricePerPerson = Double.parseDouble(fields[11]);

            System.out.printf("--- TICKET ---%n");
            System.out.printf("Reservation ID: %s%n", reservationId);
            System.out.printf("Passenger Name: %s%n", clientName);
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
            System.out.printf("Total Price: $%.2f%n%n", (passengers * pricePerPerson));
            System.out.printf("--------------%n");

        } catch (Exception e) {
            System.err.println("Failed to parse ticket details from message: " + message);
            e.printStackTrace();
        }
    }

    private void informUser(String message, boolean isApproved) {
        if (isApproved) {
            System.out.println("\nYour payment has been approved!\n");
        } else {
            System.out.println("\nYour payment was denied.\n");
        }
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