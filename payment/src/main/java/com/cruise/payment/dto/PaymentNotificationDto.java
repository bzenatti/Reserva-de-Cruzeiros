package com.cruise.payment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentNotificationDto {
    private String reservationId;
    private String status; 
}