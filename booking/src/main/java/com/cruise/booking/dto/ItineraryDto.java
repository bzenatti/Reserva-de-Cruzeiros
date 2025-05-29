package com.cruise.booking.dto;

import java.util.List;

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


    // Getters
    public String getShipName() { return shipName; }
    public String getEmbarkationPort() { return embarkationPort; }
    public String getDisembarkationPort() { return disembarkationPort; }
    public List<String> getVisitedPlaces() { return visitedPlaces; }
    public int getNights() { return nights; }
    public double getPricePerPerson() { return pricePerPerson; }
    public int getDepartureDayOfMonth() { return departureDayOfMonth; }
    public int getMaxCabins() { return maxCabins; }
    public int getAvailableCabins() { return availableCabins; }
    public int getMaxPassengers() { return maxPassengers; }
    public int getAvailablePassengers() { return availablePassengers; }

    public ItineraryDto() {}
}