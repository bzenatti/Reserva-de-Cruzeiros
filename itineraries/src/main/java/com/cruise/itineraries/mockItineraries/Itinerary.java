package com.cruise.itineraries.mockItineraries;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.cruise.itineraries.Reservation;

import lombok.Getter;

@Getter
public class Itinerary {
    private final String shipName;
    private final String embarkationPort;
    private final String disembarkationPort;
    private final List<String> visitedPlaces;
    private final int nights;
    private final double pricePerPerson;
    private final int departureDayOfMonth;
    private final int maxCabins;
    private int availableCabins;
    private final int maxPassengers;
    private int availablePassengers;

    public Itinerary(String shipName,
                     String embarkationPort,
                     String disembarkationPort,
                     List<String> visitedPlaces,
                     int nights,
                     double pricePerPerson,
                     int departureDayOfMonth,
                     int maxCabins,
                     int maxPassengers) {
        this.shipName = shipName;
        this.embarkationPort = embarkationPort;
        this.disembarkationPort = disembarkationPort;
        this.visitedPlaces = visitedPlaces;
        this.nights = nights;
        this.pricePerPerson = pricePerPerson;
        this.departureDayOfMonth = departureDayOfMonth;
        this.maxCabins = maxCabins;
        this.availableCabins = maxCabins;
        this.maxPassengers = maxPassengers;
        this.availablePassengers = maxPassengers;
    }

    public LocalDate getDepartureDate(int year, int month) {
        return LocalDate.of(year, month, departureDayOfMonth);
    }

    public boolean bookUnits(int cabinsToBook, int passengersToBook) {
        if (cabinsToBook <= this.availableCabins && passengersToBook <= this.availablePassengers) {
            this.availableCabins -= cabinsToBook;
            this.availablePassengers -= passengersToBook;
            return true;
        }
        return false;
    }

    public void releaseUnits(int cabinsToRelease, int passengersToRelease) {
        this.availableCabins += cabinsToRelease;
        if (this.availableCabins > this.maxCabins) {
            this.availableCabins = this.maxCabins;
        }
        this.availablePassengers += passengersToRelease;
        if (this.availablePassengers > this.maxPassengers) {
            this.availablePassengers = this.maxPassengers;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "Ship: %s%n" +
            "Departure Day: %d of each month%n" +
            "Embarkation: %s to %s%n" +
            "Visited: %s%n" +
            "Nights: %d | $%.2f per person%n" +
            "Cabins: %d available / %d total%n" +
            "Passengers: %d available / %d total%n",
            shipName,
            departureDayOfMonth,
            embarkationPort,
            disembarkationPort,
            String.join(", ", visitedPlaces),
            nights,
            pricePerPerson,
            availableCabins,
            maxCabins,
            availablePassengers,
            maxPassengers
        );
    }

    private static final Map<String, List<Itinerary>> itinerariesMap = new HashMap<>();
    private static final Map<String, Reservation> activeReservationsMap = new ConcurrentHashMap<>();

    public static Map<String, List<Itinerary>> getItinerariesMap() {
        return itinerariesMap;
    }

    public static void addActiveReservation(Reservation details) {
        activeReservationsMap.put(details.getReservationId(), details);
        System.out.println("Stored active reservation: " + details.getReservationId());
    }
    public static Reservation removeActiveReservation(String reservationId) {
        Reservation removed = activeReservationsMap.remove(reservationId);
        if (removed != null) {
            System.out.println("Removed active reservation: " + reservationId);
        } else {
            System.out.println("No active reservation found to remove for ID: " + reservationId);
        }
        return removed;
    }

    static {
        itinerariesMap.put("Bahamas", List.of(
            new Itinerary("Caribbean Queen", "Miami", "Nassau",
                          List.of("Nassau", "CocoCay", "Freeport"), 5, 900.0, 10, 50, 150),
            new Itinerary("Bahamas Explorer", "Orlando", "Nassau",
                          List.of("Nassau", "Bimini"), 4, 750.0, 15, 40, 120)
        ));
        itinerariesMap.put("Italy", List.of(
            new Itinerary("Mediterranean Star", "Rome", "Venice",
                          List.of("Naples", "Florence", "Venice"), 7, 1200.0, 5, 60, 180),
            new Itinerary("Italia Explorer", "Naples", "Genoa",
                          List.of("Rome", "La Spezia"), 6, 1100.0, 10, 55, 160)
        ));
        itinerariesMap.put("Brazil", List.of(
            new Itinerary("Costa do Sol", "Rio de Janeiro", "Salvador",
                          List.of("Búzios", "Ilhabela", "Salvador"), 7, 780.0, 10, 70, 200),
            new Itinerary("Nordeste Dreams", "Fortaleza", "Recife",
                          List.of("Natal", "João Pessoa", "Recife"), 6, 720.0, 18, 65, 190),
            new Itinerary("Sul Encantado", "Santos", "Porto Alegre",
                          List.of("Paranaguá", "Florianópolis", "Porto Alegre"), 5, 680.0, 20, 50, 150),
            new Itinerary("Amazônia Cruise", "Manaus", "Belém",
                          List.of("Parintins", "Santarém", "Macapá", "Belém"), 8, 950.0, 5, 45, 130)
        ));
        itinerariesMap.put("Norway", List.of(
            new Itinerary("Northern Lights Voyager", "Oslo", "Bergen",
                          List.of("Flam", "Geiranger", "Alesund"), 8, 1350.0, 3, 50, 140)
        ));
    }

    public static List<Itinerary> getMatchingItineraries(String destination,
                                                         int year,
                                                         int month,
                                                         String embarkationPort) {
        return itinerariesMap
            .getOrDefault(destination, List.of())
            .stream()
            .filter(i -> i.getEmbarkationPort().equalsIgnoreCase(embarkationPort))
            .map(i -> new Itinerary(
                i.getShipName(),
                i.getEmbarkationPort(),
                i.getDisembarkationPort(),
                i.getVisitedPlaces(),
                i.getNights(),
                i.getPricePerPerson(),
                i.getDepartureDayOfMonth(),
                i.getMaxCabins(),
                i.getMaxPassengers()
            ))
            .collect(Collectors.toList());
    }

    public static void findAvailableItineraries(String destination,
                                                int year,
                                                int month,
                                                String embarkationPort) {
        var results = getMatchingItineraries(destination, year, month, embarkationPort);
        if (results.isEmpty()) {
            System.out.println("No itineraries found for that selection.");
        } else {
            System.out.println("\nAvailable Itineraries for " + destination + " from " + embarkationPort + " in " + month + "/" + year + ":");
            results.forEach(itinerary -> {
                LocalDate date = itinerary.getDepartureDate(year, month);
                if (date != null) {
                    System.out.printf(
                        "\nShip: %s%n" +
                        "Date: %s%n" +
                        "From: %s to %s%n" +
                        "Visited: %s%n" +
                        "Nights: %d | $%.2f per person%n" +
                        "Cabins Available: %d (Max: %d)%n" +
                        "Passenger Spots Available: %d (Max: %d)%n",
                        itinerary.getShipName(),
                        date,
                        itinerary.getEmbarkationPort(),
                        itinerary.getDisembarkationPort(),
                        String.join(", ", itinerary.getVisitedPlaces()),
                        itinerary.getNights(),
                        itinerary.getPricePerPerson(),
                        itinerary.getAvailableCabins(),
                        itinerary.getMaxCabins(),
                        itinerary.getAvailablePassengers(),
                        itinerary.getMaxPassengers()
                    );
                }
            });
        }
    }
}