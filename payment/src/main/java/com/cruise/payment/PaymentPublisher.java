package com.cruise.payment;

import com.cruise.payment.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishApprovedPayment(String message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.APPROVED_PAYMENT, 
                RabbitConfig.APPROVED_PAYMENT, 
                message                        
        );
        System.out.println("Published approved payment message: " + message);
    }

    public void publishDeniedPayment(String message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.DENIED_PAYMENT, 
                RabbitConfig.DENIED_PAYMENT,   
                message                        
        );
        System.out.println("Published denied payment message: " + message);
    }
}