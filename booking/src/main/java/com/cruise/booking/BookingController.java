package com.cruise.booking;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import com.cruise.booking.dto.ItineraryDto;

@RestController
public class BookingController {

    @Autowired
    private RestTemplate restTemplate;

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
}
