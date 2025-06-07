package com.cruise.booking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.amqp.core.Message;

import com.cruise.booking.config.RabbitConfig;

import jakarta.annotation.PreDestroy;

@Component
public class SubscriberBooking {

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    private String getClientNameFromMessage(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        String[] parts = message.split(",");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(Message amqpMessage) {
        String message = new String(amqpMessage.getBody());
        String clientName = getClientNameFromMessage(message);

        try {
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            if (verifyMessage(message, signature)) {
                System.out.println("\nValid approved payment received:\n" + message);
                informUser(message, true);
                if (clientName != null) {
                    sendSseEventToClient(clientName, "payment_approved", "Your payment has been approved! Details: " + message);
                }
            } else {
                System.out.println("\nInvalid signature detected. Message discarded.");
                if (clientName != null) {
                    sendSseEventToClient(clientName, "payment_error", "Invalid payment signature received. Message discarded.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error while processing approved payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.DENIED_PAYMENT_QUEUE)
    public void listenToDeniedPayment(Message amqpMessage) {
        String message = new String(amqpMessage.getBody());
        String clientName = getClientNameFromMessage(message);

        try {
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            if (verifyMessage(message, signature)) {
                System.out.println("\nValid denied payment received:\n" + message);
                informUser(message, false);
                if (clientName != null) {
                    sendSseEventToClient(clientName, "payment_denied", "Your payment was denied. Details: " + message);
                }
            } else {
                System.out.println("\nInvalid signature detected for denied payment. Message discarded.");
                if (clientName != null) {
                    sendSseEventToClient(clientName, "payment_error", "Invalid payment signature received. Message discarded.");
                }
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
        String clientName = getClientNameFromMessage(message);
        if (clientName != null) {
            sendSseEventToClient(clientName, "ticket_generated", "Your ticket has been generated! Details: " + message);
        }
    }

    @RabbitListener(queues = RabbitConfig.PROMOTIONS_QUEUE_BOOKING)
    public void listenToPromotions(String promotionMessage) {
        System.out.println("Received promotion: " + promotionMessage);
        broadcastSseEvent("promotion", promotionMessage);
    }

    private void broadcastSseEvent(String eventName, String data) {
        sseEmitters.forEach((clientName, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                removeSseEmitter(clientName);
            }
        });
    }

    private void sendSseEventToClient(String clientName, String eventName, String data) {
        SseEmitter emitter = sseEmitters.get(clientName);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                removeSseEmitter(clientName);
            }
        }
    }

    public void addSseEmitter(String clientName, SseEmitter emitter) {
        this.sseEmitters.put(clientName, emitter);
        emitter.onCompletion(() -> this.sseEmitters.remove(clientName));
        emitter.onTimeout(() -> this.sseEmitters.remove(clientName));
        emitter.onError(e -> this.sseEmitters.remove(clientName));
    }

    public SseEmitter removeSseEmitter(String clientName) {
        return this.sseEmitters.remove(clientName);
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down SSE emitters in SubscriberBooking...");
        sseEmitters.values().forEach(SseEmitter::complete);
        System.out.println("SSE emitters in SubscriberBooking completed.");
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