package com.cruise.marketing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PROMOTION_EXCHANGE = "promotions-exchange";
    public static final String PROMOTIONS_QUEUE = "promotions-queue";

    @Bean
    public TopicExchange promotionsExchange() {
        return new TopicExchange(PROMOTION_EXCHANGE);
    }
    @Bean
    public Queue promotionsQueue() {
        return new Queue(PROMOTIONS_QUEUE, true);
    }

    @Bean
    public Binding approvedPaymentBinding(TopicExchange promotionsExchange, @Qualifier("promotionsQueue") Queue promotionsQueue) {
        return BindingBuilder.bind(promotionsQueue)
                .to(promotionsExchange)
                .with("promotions");
    }

}