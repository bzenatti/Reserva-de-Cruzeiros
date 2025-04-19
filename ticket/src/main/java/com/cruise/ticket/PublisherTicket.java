package com.cruise.ticket;

import com.cruise.ticket.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublisherTicket {

    private final RabbitTemplate rabbitTemplate;

    public PublisherTicket(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTicketGenerated(String ticketDetails) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.TICKET_GENERATED, 
                "ticket.generated",            
                ticketDetails                  
        );
        System.out.println("Published ticket-generated message: " + ticketDetails);
    }
}