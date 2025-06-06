package com.cruise.payment.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReservationDto {
    private String reservationId;
    private String clientName;
    private String destination;
    private String shipName;
    private String embarkationPort;
    private String disembarkationPort;
    private int year;
    private int month;
    private int departureDayOfMonth;
    private int numPassengers;
    private int numCabins;
    private List<String> visitedPlaces;
    private int nights;
    private double pricePerPerson;
    public ReservationDto () {}
}
