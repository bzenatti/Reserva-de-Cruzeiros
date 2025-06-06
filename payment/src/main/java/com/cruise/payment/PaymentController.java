package com.cruise.payment;

import com.cruise.payment.dto.PaymentLinkDto;
import com.cruise.payment.dto.PaymentNotificationDto;
import com.cruise.payment.dto.ReservationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentPublisher paymentPublisher;

    private static final Map<String, ReservationDto> pendingPayments = new ConcurrentHashMap<>();

    @PostMapping("/request-payment")
    public ResponseEntity<PaymentLinkDto> createPaymentLink(@RequestBody ReservationDto reservationDetails) {
        String reservationId = reservationDetails.getReservationId();
        if (reservationId == null || reservationId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        pendingPayments.put(reservationId, reservationDetails);
        System.out.println("Stored pending payment for reservation ID: " + reservationId);

        double amount = reservationDetails.getPricePerPerson() * reservationDetails.getNumPassengers();
        String paymentLinkUrl = String.format(
            "http://localhost:8083/payment.html?reservationId=%s&amount=%.2f",
            reservationId,
            amount
        );

        return ResponseEntity.ok(new PaymentLinkDto(paymentLinkUrl));
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> receivePaymentNotification(@RequestBody PaymentNotificationDto notification) throws Exception {
        String reservationId = notification.getReservationId();
        
        ReservationDto reservationDetails = pendingPayments.get(reservationId);

        if (reservationDetails == null) {
            System.err.println("Webhook received for an unknown or already processed reservation ID: " + reservationId);
            return ResponseEntity.badRequest().build();
        }

        LocalDate finalDepartureDate = LocalDate.of(reservationDetails.getYear(), reservationDetails.getMonth(), reservationDetails.getDepartureDayOfMonth());
        String message = String.join(",",
            reservationDetails.getReservationId(),
            reservationDetails.getClientName(),
            reservationDetails.getDestination(),
            reservationDetails.getShipName(),
            reservationDetails.getEmbarkationPort(),
            reservationDetails.getDisembarkationPort(),
            finalDepartureDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            String.valueOf(reservationDetails.getNumPassengers()),
            String.valueOf(reservationDetails.getNumCabins()),
            String.join(";", reservationDetails.getVisitedPlaces() != null ? reservationDetails.getVisitedPlaces() : Collections.emptyList()),
            String.valueOf(reservationDetails.getNights()),
            String.format("%.2f", reservationDetails.getPricePerPerson()).replace(",", ".")
        );
        
        if ("approved".equalsIgnoreCase(notification.getStatus())) {
            paymentPublisher.publishApprovedPayment(message);
        } else if ("denied".equalsIgnoreCase(notification.getStatus())) {
            paymentPublisher.publishDeniedPayment(message);
        }

        pendingPayments.remove(reservationId);
        
        return ResponseEntity.ok().build();
    }
}