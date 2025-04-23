package com.cruise.ticket;

import com.cruise.ticket.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class SubscriberTicket {

    private final PublisherTicket publisherTicket;

    public SubscriberTicket(PublisherTicket publisherTicket) {
        this.publisherTicket = publisherTicket;
    }

    @RabbitListener(queues = RabbitConfig.APPROVED_PAYMENT_QUEUE)
    public void listenToApprovedPayment(Message amqpMessage) {
        try {
            String message = new String(amqpMessage.getBody());
            String signature = (String) amqpMessage.getMessageProperties().getHeaders().get("signature");

            boolean isValid = verifyMessage(message, signature);
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            if (isValid) {
                System.out.println("\nReceived valid approved payment: " + message);

                String ticketDetails = generateTicket(message);
                publisherTicket.publishTicketGenerated(ticketDetails);
            } else {
                System.out.println("\nInvalid signature. Discarding the message.");
            }
        } catch (Exception e) {
            System.err.println("Error processing approved payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateTicket(String message) {
        System.out.println("\nGenerating ticket for booking: " + message);
        return  message;
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