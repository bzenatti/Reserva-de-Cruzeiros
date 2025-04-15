package com.cruise.marketing.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PROMOTION_EXCHANGE = "promotions-exchange";

    @Bean
    public TopicExchange promotionsExchange() {
        return new TopicExchange(PROMOTION_EXCHANGE);
    }
}
