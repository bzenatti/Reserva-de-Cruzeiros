package com.cruise.booking;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.cruise.booking.dto.ItineraryDto;

@SpringBootApplication
public class BookingApplication implements CommandLineRunner {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        startInterface();
    }

    public void startInterface() {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("Welcome to Cruise Booking System\n");

            System.out.print("Enter your name:");
            String username = scanner.nextLine().trim();

            System.out.print("\nChoose a destination (Bahamas, Italy, Brazil, Norway): ");
            String destination = scanner.nextLine().trim();

            System.out.print("\nEnter a departure year (YYYY): ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("\nEnter a departure month (1–12): ");
            int month = Integer.parseInt(scanner.nextLine().trim());

            Map<String, List<String>> portsByDest = new HashMap<>();
            portsByDest.put("Bahamas", List.of("Miami", "Orlando"));
            portsByDest.put("Italy",   List.of("Rome", "Naples"));
            portsByDest.put("Brazil",  List.of("Rio de Janeiro", "Fortaleza", "Santos", "Manaus"));
            portsByDest.put("Norway",  List.of("Oslo"));

            List<String> ports = portsByDest.getOrDefault(destination, Collections.emptyList());
            
            if (ports.isEmpty()) {
                System.out.println("Invalid destination or no ports configured for this destination.");
                return;
            }

            System.out.println("\nAvailable embarkation ports for " + destination + ":");
            for (int i = 0; i < ports.size(); i++) {
                System.out.printf("[%d] %s%n", i + 1, ports.get(i));
            }
            System.out.print("\nSelect port (number): ");
            int portIdx = Integer.parseInt(scanner.nextLine().trim());
            if (portIdx < 1 || portIdx > ports.size()) {
                System.out.println("Invalid port selection.");
                return;
            }
            String embarkationPort = ports.get(portIdx - 1);

            PublisherBooking publisher = new PublisherBooking(rabbitTemplate);
            
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

            if (baseMatches.isEmpty()) {
                System.out.println("\nNo itineraries found for " 
                    + destination + " in " + year + "-" + month 
                    + " from " + embarkationPort + ".");
                return;
            }

            System.out.println("\nAvailable Itineraries:");
            for (int i = 0; i < baseMatches.size(); i++) {
                ItineraryDto it = baseMatches.get(i);
                System.out.printf(
                    "\n[%d] Ship: %s%n" +
                    "    Departs every month on: day %d%n" +
                    "    From: %s to %s%n" +
                    "    Visited: %s%n" +
                    "    Nights: %d%n" +
                    "    Price per person: $%.2f%n" +
                    "    Cabins Available: %d (Max: %d)%n" +
                    "    Passenger Spots Available: %d (Max: %d)%n",
                    i + 1,
                    it.getShipName(),
                    it.getDepartureDayOfMonth(),
                    it.getEmbarkationPort(),
                    it.getDisembarkationPort(),
                    String.join(", ", it.getVisitedPlaces()),
                    it.getNights(),
                    it.getPricePerPerson(),
                    it.getAvailableCabins(),
                    it.getMaxCabins(),
                    it.getAvailablePassengers(),
                    it.getMaxPassengers()
                );
            }

            System.out.print("\nSelect itinerary number to reserve: ");
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice < 1 || choice > baseMatches.size()) {
                System.out.println("Invalid selection.");
                return;
            }
            ItineraryDto selected = baseMatches.get(choice - 1);

            LocalDate finalDeparture;
            try {
                 finalDeparture = LocalDate.of(year, month, selected.getDepartureDayOfMonth());
            } catch (java.time.DateTimeException e) {
                System.out.println("Invalid departure day for the selected month/year for this itinerary: " + e.getMessage());
                return;
            }
            
            if (finalDeparture == null) {
                 System.out.println("Invalid departure date. Try a different month or check itinerary details.");
                 return;
            }


            System.out.println("\nFinal departure date: " + finalDeparture);

            System.out.print("\nEnter number of passengers: ");
            int passengers = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("\nEnter number of cabins: ");
            int cabins = Integer.parseInt(scanner.nextLine().trim());

            if (cabins <= 0 || passengers <= 0) {
                System.out.println("Number of cabins and passengers must be greater than zero.");
                return;
            }

            if (cabins > selected.getAvailableCabins()) {
                System.out.println("Not enough cabins available. Requested: " + cabins + ", Available: " + selected.getAvailableCabins());
                return;
            }
            if (passengers > selected.getAvailablePassengers()) {
                System.out.println("Not enough passenger capacity available. Requested: " + passengers + ", Available: " + selected.getAvailablePassengers());
                return;
            }
            
            String itineraryMessage = String.join(",",
                username,
                destination,
                selected.getShipName(),
                selected.getEmbarkationPort(),
                selected.getDisembarkationPort(),
                finalDeparture.toString(),
                String.valueOf(passengers),
                String.valueOf(cabins),
                String.join(";", selected.getVisitedPlaces()),
                String.valueOf(selected.getNights()),
                String.format("%.2f", selected.getPricePerPerson())
            );

            publisher.sendBooking(itineraryMessage);

            System.out.printf("Reservation request sent. Please proceed to the payment step if applicable.\n");

        } catch (NumberFormatException e) {
            System.err.println("Invalid number input. Please enter valid numbers where required.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }
}