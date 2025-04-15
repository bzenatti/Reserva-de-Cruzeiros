package com.cruise.marketing.publisher;

import com.cruise.marketing.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PromotionPublisher {

    private final RabbitTemplate rabbitTemplate;

    private final AtomicInteger spCount = new AtomicInteger();
    private final AtomicInteger rjCount = new AtomicInteger();
    private final AtomicInteger baCount = new AtomicInteger();

    public PromotionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void sendPromotion(String destination, String promotionMessage) {
        String routingKey = "promotions." + destination;
        rabbitTemplate.convertAndSend(RabbitConfig.PROMOTION_EXCHANGE, routingKey, promotionMessage);
        System.out.printf("[%s] Sent to %s: %s%n", LocalTime.now(), routingKey, promotionMessage);
    }

    // sp every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void sendToSP() {
        String message = "Promo SP #" + spCount.incrementAndGet();
        sendPromotion("sp", message);
    }

    // rj every 8 seconds
    @Scheduled(fixedRate = 8000)
    public void sendToRJ() {
        String message = "Promo RJ #" + rjCount.incrementAndGet();
        sendPromotion("rj", message);
    }

    // ba every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void sendToBA() {
        String message = "Promo BA #" + baCount.incrementAndGet();
        sendPromotion("ba", message);
    }
}
