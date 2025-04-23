package com.cruise.booking;

import com.cruise.booking.itineraries.Itinerary;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class BookingApplication implements CommandLineRunner {

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("Welcome to Cruise Booking System");

            System.out.print("Enter your name: ");
            String username = scanner.nextLine().trim();

            System.out.print("Choose a destination (Bahamas, Italy, Brazil, Norway): ");
            String destination = scanner.nextLine().trim();

            System.out.print("Enter a departure year (YYYY): ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter a departure month (1–12): ");
            int month = Integer.parseInt(scanner.nextLine().trim());

            Map<String, List<String>> portsByDest = new HashMap<>();
            portsByDest.put("Bahamas", List.of("Miami", "Orlando"));
            portsByDest.put("Italy",   List.of("Rome", "Naples"));
            portsByDest.put("Brazil",  List.of("Rio de Janeiro", "Fortaleza", "Santos", "Manaus"));
            portsByDest.put("Norway",  List.of("Oslo"));

            List<String> ports = portsByDest.getOrDefault(destination, List.of());
            
            System.out.println("\nAvailable embarkation ports for " + destination + ":");
            for (int i = 0; i < ports.size(); i++) {
                System.out.printf("[%d] %s%n", i + 1, ports.get(i));
            }
            System.out.print("Select port (number): ");
            int portIdx = Integer.parseInt(scanner.nextLine().trim());
            if (portIdx < 1 || portIdx > ports.size()) {
                System.out.println("Invalid port selection.");
                return;
            }
            String embarkationPort = ports.get(portIdx - 1);

            PublisherBooking publisher = new PublisherBooking(rabbitTemplate);
            List<Itinerary> baseMatches = Itinerary.getMatchingItineraries(destination, year, month, embarkationPort);

            if (baseMatches.isEmpty()) {
                System.out.println("\nNo itineraries found for " 
                    + destination + " in " + year + "-" + month 
                    + " from " + embarkationPort + ".");
                return;
            }

            System.out.println("\nAvailable Itineraries:");
            for (int i = 0; i < baseMatches.size(); i++) {
                Itinerary it = baseMatches.get(i);
                System.out.printf(
                    "\n[%d] Ship: %s%n" +
                    "    Departs every month on: day %d%n" +
                    "    From: %s to %s%n" +
                    "    Visited: %s%n" +
                    "    Nights: %d%n" +
                    "    Price per person: $%.2f%n",
                    i + 1,
                    it.getShipName(),
                    it.getDepartureDayOfMonth(),
                    it.getEmbarkationPort(),
                    it.getDisembarkationPort(),
                    String.join(", ", it.getVisitedPlaces()),
                    it.getNights(),
                    it.getPricePerPerson()
                );
            }

            System.out.print("\nSelect itinerary number to reserve: ");
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice < 1 || choice > baseMatches.size()) {
                System.out.println("Invalid selection.");
                return;
            }
            Itinerary selected = baseMatches.get(choice - 1);

            LocalDate finalDeparture = selected.getDepartureDate(year, month);
            if (finalDeparture == null) {
                System.out.println("Invalid departure date. Try a different month.");
                return;
            }

            System.out.println("Final departure date: " + finalDeparture);

            System.out.print("Enter number of passengers: ");
            int passengers = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter number of cabins: ");
            int cabins = Integer.parseInt(scanner.nextLine().trim());

            // The itinerary message is separated by commas, with fields with more than one item separated by `;`
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

            System.out.printf("Goes to the payment tab to proceed\n");

        } finally {
            scanner.close();
        }
    }
}
