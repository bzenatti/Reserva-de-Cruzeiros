package com.cruise.itineraries;

import com.cruise.itineraries.config.RabbitConfig;
import com.cruise.itineraries.mockItineraries.Itinerary;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ItinerarySubscriber {

    @RabbitListener(queues = RabbitConfig.BOOKING_CREATED_QUEUE)
    public void handleBookingCreated(String message) {
        System.out.println("Received booking_created message: " + message);
        try {
            String[] parts = message.split(",");
            if (parts.length < 12) {
                System.err.println("Malformed booking_created message (expected at least 12 parts): " + message);
                return;
            }

            String reservationId = parts[0];
            String destination = parts[2];
            String shipName = parts[3];
            String embarkationPort = parts[4];
            String finalDepartureDateStr = parts[6];
            int numPassengers = Integer.parseInt(parts[7]);
            int numCabins = Integer.parseInt(parts[8]);

            LocalDate finalDepartureDate = LocalDate.parse(finalDepartureDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            int departureDayOfMonth = finalDepartureDate.getDayOfMonth();
            int year = finalDepartureDate.getYear();
            int month = finalDepartureDate.getMonthValue();

            Map<String, List<Itinerary>> itinerariesMap = Itinerary.getItinerariesMap();
            List<Itinerary> destinationItineraries = itinerariesMap.get(destination);

            if (destinationItineraries == null) {
                System.err.println("No itineraries found for destination: " + destination);
                return;
            }

            Itinerary matchedItinerary = findItinerary(destinationItineraries, shipName, embarkationPort, departureDayOfMonth);

            if (matchedItinerary != null) {
                boolean success = matchedItinerary.bookUnits(numCabins, numPassengers);
                if (success) {
                    System.out.println("Successfully updated itinerary: " + shipName + ". Cabins reduced by " + numCabins + ", Passengers by " + numPassengers);
                    System.out.println("New availability - Cabins: " + matchedItinerary.getAvailableCabins() + ", Passengers: " + matchedItinerary.getAvailablePassengers());

                    Reservation details = new Reservation(
                        reservationId, destination, shipName, embarkationPort,
                        departureDayOfMonth, year, month, numCabins, numPassengers
                    );
                    Itinerary.addActiveReservation(details);
                } else {
                    System.err.println("Failed to book units for itinerary: " + shipName + ". Insufficient availability.");
                }
            } else {
                System.err.println("Could not find matching itinerary for (booking created): " + shipName + " from " + embarkationPort + " on day " + departureDayOfMonth + " for destination " + destination);
            }
        } catch (Exception e) {
            System.err.println("Error processing booking_created message: " + message);
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = RabbitConfig.BOOKING_DELETED_QUEUE)
    public void handleBookingDeleted(String reservationIdMessage) { 
        System.out.println("Received booking_deleted message for reservation ID: " + reservationIdMessage);
        try {
            String reservationId = reservationIdMessage.trim(); 

            Reservation details = Itinerary.removeActiveReservation(reservationId);

            if (details != null) {
                Map<String, List<Itinerary>> itinerariesMap = Itinerary.getItinerariesMap();
                List<Itinerary> destinationItineraries = itinerariesMap.get(details.getDestination());

                Itinerary matchedItinerary = findItinerary(destinationItineraries, details.getShipName(),
                                                           details.getEmbarkationPort(), details.getDepartureDayOfMonth());
                matchedItinerary.releaseUnits(details.getNumCabinsBooked(), details.getNumPassengersBooked());
                System.out.println("Successfully restored units for itinerary: " + details.getShipName() + " (Reservation ID: " + reservationId + "). Cabins restored by " + details.getNumCabinsBooked() + ", Passengers by " + details.getNumPassengersBooked());
                System.out.println("New availability - Cabins: " + matchedItinerary.getAvailableCabins() + ", Passengers: " + matchedItinerary.getAvailablePassengers());
                
            } else {
                System.err.println("No active reservation details found for ID: '" + reservationId + "'. Cannot process cancellation. Message might have been processed already or ID is incorrect.");
            }

        } catch (Exception e) {
            System.err.println("Error processing booking_deleted message (Reservation ID: " + reservationIdMessage + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Itinerary findItinerary(List<Itinerary> itineraries, String shipName, String embarkationPort, int departureDayOfMonth) {
        for (Itinerary itinerary : itineraries) {
            if (itinerary.getShipName().equalsIgnoreCase(shipName) &&
                itinerary.getEmbarkationPort().equalsIgnoreCase(embarkationPort) &&
                itinerary.getDepartureDayOfMonth() == departureDayOfMonth) {
                return itinerary;
            }
        }
        return null;
    }
}