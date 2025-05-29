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

    private final AtomicInteger italyCount = new AtomicInteger();
    private final AtomicInteger bahamasCount = new AtomicInteger();
    private final AtomicInteger brazilCount = new AtomicInteger();
    private final AtomicInteger norwayCount = new AtomicInteger();

    public PromotionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void sendPromotion(String promotionMessage) {
        String routingKey = "promotions" ;
        rabbitTemplate.convertAndSend(RabbitConfig.PROMOTION_EXCHANGE, routingKey, promotionMessage);
        System.out.printf("[%s] Sent to %s: %s%n", LocalTime.now(), routingKey, promotionMessage);
    }

    // Italy promotions every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void sendToItaly() {
        String message = "Promo Italy #" + italyCount.incrementAndGet();
        sendPromotion(message);
    }

    // Bahamas promotions every 8 seconds
    @Scheduled(fixedRate = 8000)
    public void sendToBahamas() {
        String message = "Promo Bahamas #" + bahamasCount.incrementAndGet();
        sendPromotion(message);
    }

    // Brazil promotions every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void sendToBrazil() {
        String message = "Promo Brazil #" + brazilCount.incrementAndGet();
        sendPromotion(message);
    }

    // Norway promotions every 12 seconds
    @Scheduled(fixedRate = 12000)
    public void sendToNorway() {
        String message = "Promo Norway #" + norwayCount.incrementAndGet();
        sendPromotion(message);
    }
}