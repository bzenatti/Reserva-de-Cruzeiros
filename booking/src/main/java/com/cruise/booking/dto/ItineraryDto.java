package com.cruise.booking.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class ItineraryDto {
    private String shipName;
    private String embarkationPort;
    private String disembarkationPort;
    private List<String> visitedPlaces;
    private int nights;
    private double pricePerPerson;
    private int departureDayOfMonth;
    private int maxCabins;
    private int availableCabins;
    private int maxPassengers;
    private int availablePassengers;

    public ItineraryDto() {}
}