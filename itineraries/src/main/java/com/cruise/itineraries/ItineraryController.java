package com.cruise.itineraries;

import com.cruise.itineraries.mockItineraries.Itinerary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries") 
public class ItineraryController {

    @GetMapping
    public List<Itinerary> getAvailableItineraries(
            @RequestParam String destination,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String embarkationPort) {

        List<Itinerary> matches = Itinerary.getMatchingItineraries(destination, year, month, embarkationPort);

        return matches;
    }
}