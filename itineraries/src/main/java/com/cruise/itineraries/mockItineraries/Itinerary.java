package com.cruise.itineraries.mockItineraries;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Itinerary {
    private final String shipName;
    private final String embarkationPort;
    private final String disembarkationPort;
    private final List<String> visitedPlaces;
    private final int nights;
    private final double pricePerPerson;
    private final int departureDayOfMonth;

    public Itinerary(String shipName,
                     String embarkationPort,
                     String disembarkationPort,
                     List<String> visitedPlaces,
                     int nights,
                     double pricePerPerson,
                     int departureDayOfMonth) {
        this.shipName = shipName;
        this.embarkationPort = embarkationPort;
        this.disembarkationPort = disembarkationPort;
        this.visitedPlaces = visitedPlaces;
        this.nights = nights;
        this.pricePerPerson = pricePerPerson;
        this.departureDayOfMonth = departureDayOfMonth;
    }

    public String getShipName()            { return shipName; }
    public String getEmbarkationPort()     { return embarkationPort; }
    public String getDisembarkationPort()  { return disembarkationPort; }
    public List<String> getVisitedPlaces() { return visitedPlaces; }
    public int getNights()                 { return nights; }
    public double getPricePerPerson()      { return pricePerPerson; }
    public int getDepartureDayOfMonth()    { return departureDayOfMonth; }

    public LocalDate getDepartureDate(int year, int month) {
        return LocalDate.of(year, month, departureDayOfMonth);
    }

    @Override
    public String toString() {
        return String.format(
            "Ship: %s%n" +
            "Departure Day: %d of each month%n" +
            "Embarkation: %s to %s%n" +
            "Visited: %s%n" +
            "Nights: %d | $%.2f per person%n",
            shipName,
            departureDayOfMonth,
            embarkationPort,
            disembarkationPort,
            String.join(", ", visitedPlaces),
            nights,
            pricePerPerson
        );
    }

    private static final Map<String, List<Itinerary>> itinerariesMap = new HashMap<>();

    static {
        itinerariesMap.put("Bahamas", List.of(
            new Itinerary("Caribbean Queen", "Miami", "Nassau",
                          List.of("Nassau", "CocoCay", "Freeport"), 5, 900.0, 10),
            new Itinerary("Bahamas Explorer", "Orlando", "Nassau",
                          List.of("Nassau", "Bimini"), 4, 750.0, 15)
        ));
        itinerariesMap.put("Italy", List.of(
            new Itinerary("Mediterranean Star", "Rome", "Venice",
                          List.of("Naples", "Florence", "Venice"), 7, 1200.0, 5),
            new Itinerary("Italia Explorer", "Naples", "Genoa",
                          List.of("Rome", "La Spezia"), 6, 1100.0, 10)
        ));
        itinerariesMap.put("Brazil", List.of(
            new Itinerary("Costa do Sol", "Rio de Janeiro", "Salvador",
                          List.of("Búzios", "Ilhabela", "Salvador"), 7, 780.0, 10),
            new Itinerary("Nordeste Dreams", "Fortaleza", "Recife",
                          List.of("Natal", "João Pessoa", "Recife"), 6, 720.0, 18),
            new Itinerary("Sul Encantado", "Santos", "Porto Alegre",
                          List.of("Paranaguá", "Florianópolis", "Porto Alegre"), 5, 680.0, 20),
            new Itinerary("Amazônia Cruise", "Manaus", "Belém",
                          List.of("Parintins", "Santarém", "Macapá", "Belém"), 8, 950.0, 5)
        ));
        itinerariesMap.put("Norway", List.of(
            new Itinerary("Northern Lights Voyager", "Oslo", "Bergen",
                          List.of("Flam", "Geiranger", "Alesund"), 8, 1350.0, 3)
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
                i.getDepartureDayOfMonth()
            ))
            .toList();
    }

    public static void findAvailableItineraries(String destination,
                                                int year,
                                                int month,
                                                String embarkationPort) {
        var results = getMatchingItineraries(destination, year, month, embarkationPort);
        if (results.isEmpty()) {
            System.out.println("No itineraries found for that selection.");
        } else {
            System.out.println("Available Itineraries:");
            results.forEach(itinerary -> {
                LocalDate date = itinerary.getDepartureDate(year, month);
                System.out.printf(
                    "\nShip: %s%n" +
                    "Date: %s%n" +
                    "From: %s to %s%n" +
                    "Visited: %s%n" +
                    "Nights: %d | $%.2f per person%n",
                    itinerary.getShipName(),
                    date,
                    itinerary.getEmbarkationPort(),
                    itinerary.getDisembarkationPort(),
                    String.join(", ", itinerary.getVisitedPlaces()),
                    itinerary.getNights(),
                    itinerary.getPricePerPerson()
                );
            });
        }
    }
}
