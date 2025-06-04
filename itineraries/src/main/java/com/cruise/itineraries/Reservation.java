package com.cruise.itineraries;

import lombok.Getter;

@Getter
public class Reservation {
    private String reservationId;
    private String destination;
    private String shipName;
    private String embarkationPort;
    private int departureDayOfMonth;
    private int year; 
    private int month; 
    private int numCabinsBooked;
    private int numPassengersBooked;

    public Reservation(String reservationId, String destination, String shipName,
                                             String embarkationPort, int departureDayOfMonth, int year, int month,
                                             int numCabinsBooked, int numPassengersBooked) {
        this.reservationId = reservationId;
        this.destination = destination;
        this.shipName = shipName;
        this.embarkationPort = embarkationPort;
        this.departureDayOfMonth = departureDayOfMonth;
        this.year = year;
        this.month = month;
        this.numCabinsBooked = numCabinsBooked;
        this.numPassengersBooked = numPassengersBooked;
    }

    @Override
    public String toString() {
        return "ReservationDetailsForCancellation{" +
               "reservationId='" + reservationId + '\'' +
               ", destination='" + destination + '\'' +
               ", shipName='" + shipName + '\'' +
               ", embarkationPort='" + embarkationPort + '\'' +
               ", departureDayOfMonth=" + departureDayOfMonth +
               ", year=" + year +
               ", month=" + month +
               ", numCabinsBooked=" + numCabinsBooked +
               ", numPassengersBooked=" + numPassengersBooked +
               '}';
    }
}