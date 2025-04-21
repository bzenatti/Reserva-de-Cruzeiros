package com.cruise.payment;

import com.cruise.payment.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
public class PaymentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishApprovedPayment(String message) throws Exception {
        String signature = signMessage(message);

        rabbitTemplate.convertAndSend(
                RabbitConfig.APPROVED_PAYMENT, 
                RabbitConfig.APPROVED_PAYMENT, 
                message,                       
                m -> {
                    m.getMessageProperties().setHeader("signature", signature); // Add signature to header
                    return m;
                }
        );
        System.out.println("\n\nPublished approved payment message: \n" + message);
        // System.out.println("Signature: " + signature);
    }

    public void publishDeniedPayment(String message) throws Exception {
        String signature = signMessage(message);

        rabbitTemplate.convertAndSend(
                RabbitConfig.DENIED_PAYMENT, 
                RabbitConfig.DENIED_PAYMENT, 
                message,                     
                m -> {
                    m.getMessageProperties().setHeader("signature", signature); 
                    return m;
                }
        );
        System.out.println("\n\nPublished denied payment message: \n" + message);
        System.out.println("Signature: " + signature);
    }

    private String signMessage(String message) throws Exception {
        // Gets the key and transform into a privatekey object
        byte[] privateKeyBytes = Files.readAllBytes(Paths.get("payment-private.key"));
        // byte[] privateKeyBytes = Files.readAllBytes(Paths.get("wrong-payment-private.key"));
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);

        signature.update(message.getBytes());
        byte[] signedBytes = signature.sign();

        return Base64.getEncoder().encodeToString(signedBytes);
    }
}