package com.cruise.booking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;

import com.cruise.booking.dto.ItineraryDto;
import com.cruise.booking.dto.ReservationDto;

import jakarta.annotation.PreDestroy;

import com.cruise.booking.dto.PaymentLinkDto;

@RestController
public class BookingController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PublisherBooking publisherBooking;

    @Autowired
    private SubscriberBooking subscriberBooking;

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/index.html");
    }

    @GetMapping("/available-itineraries")
    List<ItineraryDto> getAvailableItineraries(
            @RequestHeader("destination") String destination,
            @RequestHeader("year") int year,
            @RequestHeader("month") int month,
            @RequestHeader("embarkationPort") String embarkationPort)
    {
        
        String itinerariesApiUrl = "http://localhost:8081/api/itineraries"; 
        String url = String.format("%s?destination=%s&year=%d&month=%d&embarkationPort=%s",
                                       itinerariesApiUrl, destination, year, month, embarkationPort);
        List<ItineraryDto> baseMatches;
        
        try {
            ResponseEntity<ItineraryDto[]> response = restTemplate.getForEntity(url, ItineraryDto[].class);
            if (response.getBody() != null) {
                baseMatches = Arrays.asList(response.getBody());
            } else {
                baseMatches = Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching itineraries: " + e.getMessage());
            baseMatches = Collections.emptyList();
        }

        return baseMatches;
    }

    @PostMapping("/make-reservation")
    public ResponseEntity<?> makeReservation(@RequestBody ReservationDto reservationRequest) {
        try {
            if (reservationRequest == null || reservationRequest.getClientName() == null || reservationRequest.getClientName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Client name is required.");
            }
            if (reservationRequest.getShipName() == null || reservationRequest.getShipName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Ship name (selected itinerary) is required.");
            }
            if (reservationRequest.getNumPassengers() <= 0 || reservationRequest.getNumCabins() <= 0) {
                return ResponseEntity.badRequest().body("Number of passengers and cabins must be greater than zero.");
            }

            LocalDate finalDepartureDate;
            try {
                finalDepartureDate = LocalDate.of(reservationRequest.getYear(), reservationRequest.getMonth(), reservationRequest.getDepartureDayOfMonth());
            } catch (java.time.DateTimeException e) {
                System.err.println("Invalid departure date components: " + reservationRequest.getYear() + "-" + reservationRequest.getMonth() + "-" + reservationRequest.getDepartureDayOfMonth() + ". Error: " + e.getMessage());
                return ResponseEntity.badRequest().body("Invalid departure date: " + e.getMessage());
            }

            String reservationId = UUID.randomUUID().toString();
            reservationRequest.setReservationId(reservationId);

            String itineraryMessage = String.join(",",
                reservationId,
                reservationRequest.getClientName(),
                reservationRequest.getDestination(),
                reservationRequest.getShipName(),
                reservationRequest.getEmbarkationPort(),
                reservationRequest.getDisembarkationPort(),
                finalDepartureDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                String.valueOf(reservationRequest.getNumPassengers()),
                String.valueOf(reservationRequest.getNumCabins()),
                String.join(";", reservationRequest.getVisitedPlaces() != null ? reservationRequest.getVisitedPlaces() : Collections.emptyList()),
                String.valueOf(reservationRequest.getNights()),
                String.format("%.2f", reservationRequest.getPricePerPerson()).replace(",", ".")
            );

            publisherBooking.sendBookingCreated(itineraryMessage);
            System.out.println("Reservation request successfully processed. Reservation ID: " + reservationId + ". Message sent for: " + reservationRequest.getClientName() + " on ship " + reservationRequest.getShipName());

            String paymentLink = "";
            try {
                String paymentServiceUrl = "http://localhost:8083/api/payment/request-payment";

                ResponseEntity<PaymentLinkDto> paymentResponse = restTemplate.postForEntity(
                    paymentServiceUrl,
                    reservationRequest,
                    PaymentLinkDto.class
                );

                if (paymentResponse.getStatusCode() == HttpStatus.OK && paymentResponse.getBody() != null) {
                    paymentLink = paymentResponse.getBody().getPaymentLink();
                    System.out.println("Successfully retrieved payment link: " + paymentLink);
                }
            } catch (Exception e) {
                System.err.println("CRITICAL: Could not retrieve payment link. Error: " + e.getMessage());
            }

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Reservation request sent. Please use the link to complete payment.");
            responseBody.put("reservationId", reservationId);
            responseBody.put("paymentLink", paymentLink);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            System.err.println("Error processing reservation request: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Error processing reservation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<?> cancelReservation(@PathVariable String reservationId) {
        try {
            if (reservationId == null || reservationId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Reservation ID is required.");
            }

            publisherBooking.sendBookingDeleted(reservationId);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Reservation cancellation request sent successfully for ID: " + reservationId);
            responseBody.put("reservationId", reservationId);
            
            System.out.println("Cancellation request processed for Reservation ID: " + reservationId + ".");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            System.err.println("Error processing cancellation request for reservation ID " + reservationId + ": " + e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Error processing cancellation request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
    
    @GetMapping(value = "/subscribe-notifications/{clientName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotifications(@PathVariable String clientName) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); 
        
        subscriberBooking.addSseEmitter(emitter);
        sseEmitters.put(clientName, emitter);

        emitter.onCompletion(() -> {
            subscriberBooking.removeSseEmitter(emitter);
            sseEmitters.remove(clientName);
        });
        emitter.onTimeout(() -> {
            subscriberBooking.removeSseEmitter(emitter);
            sseEmitters.remove(clientName);
            emitter.complete();
        });
        emitter.onError(e -> {
            subscriberBooking.removeSseEmitter(emitter);
            sseEmitters.remove(clientName);
        });
        
        try {
            emitter.send(SseEmitter.event().name("connection_established").data("SSE connection established for " + clientName));
        } catch (Exception e) {
            System.err.println("Error sending initial connection event for " + clientName + ": " + e.getMessage());
        }

        System.out.println("Client " + clientName + " subscribed for SSE notifications.");
        return emitter;
    }

    @PostMapping("/unsubscribe-notifications/{clientName}")
    public ResponseEntity<?> unsubscribeFromNotifications(@PathVariable String clientName) {
        SseEmitter emitter = sseEmitters.remove(clientName);
        if (emitter != null) {
            subscriberBooking.removeSseEmitter(emitter);
            emitter.complete();
            System.out.println("Client " + clientName + " unsubscribed from SSE notifications.");
            return ResponseEntity.ok("Unsubscribed " + clientName + " successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client " + clientName + " not found or not subscribed.");
        }
    }
    
    @PreDestroy
    public void shutdown() {
        System.out.println("Shutting down SSE emitters...");
        sseEmitters.values().forEach(SseEmitter::complete);
        System.out.println("SSE emitters completed.");
    }
}
