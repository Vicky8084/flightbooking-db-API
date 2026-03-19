package db_api.db_api.dto;


import db_api.db_api.model.Flight;
import lombok.Data;

import java.util.List;

public class ConnectingFlightDTO {
    private List<Flight> segments;
    private int totalDuration; // minutes
    private int totalLayoverTime; // minutes
    private double totalPrice;
    private String sourceCode;
    private String destinationCode;
    private String sourceCity;
    private String destinationCity;
    private int numberOfStops;
    private List<String> flightNumbers;
    private List<String> airlines;
    private List<String> layoverAirports;
    private List<Integer> layoverDurations;



        // Constructor
    public ConnectingFlightDTO(List<Flight> segments) {
        this.segments = segments;
        this.numberOfStops = segments.size() - 1;
        calculateTotals();
        extractFlightNumbers();
        extractAirlines();
        extractLayoverInfo();
    }

    private void calculateTotals() {
        this.totalDuration = 0;
        this.totalLayoverTime = 0;
        this.totalPrice = 0;

        for (int i = 0; i < segments.size(); i++) {
            Flight flight = segments.get(i);

            // Add flight duration
            totalDuration += flight.getDuration();

            // Add flight price (using economy as base)
            totalPrice += flight.getBasePriceEconomy();

            // Calculate layover between flights
            if (i < segments.size() - 1) {
                Flight nextFlight = segments.get(i + 1);
                long layover = java.time.Duration.between(
                        flight.getArrivalTime(),
                        nextFlight.getDepartureTime()
                ).toMinutes();
                totalLayoverTime += layover;
                totalDuration += layover;
            }
        }

        Flight firstFlight = segments.get(0);
        Flight lastFlight = segments.get(segments.size() - 1);

        this.sourceCode = firstFlight.getSourceAirport().getCode();
        this.sourceCity = firstFlight.getSourceAirport().getCity();
        this.destinationCode = lastFlight.getDestinationAirport().getCode();
        this.destinationCity = lastFlight.getDestinationAirport().getCity();
    }

    private void extractFlightNumbers() {
        this.flightNumbers = segments.stream()
                .map(Flight::getFlightNumber)
                .collect(java.util.stream.Collectors.toList());
    }

    private void extractAirlines() {
        this.airlines = segments.stream()
                .map(f -> f.getAircraft().getAirline().getName())
                .collect(java.util.stream.Collectors.toList());
    }

    private void extractLayoverInfo() {
        this.layoverAirports = new java.util.ArrayList<>();
        this.layoverDurations = new java.util.ArrayList<>();

        for (int i = 0; i < segments.size() - 1; i++) {
            Flight current = segments.get(i);
            Flight next = segments.get(i + 1);

            layoverAirports.add(current.getDestinationAirport().getCode());

            long layover = java.time.Duration.between(
                    current.getArrivalTime(),
                    next.getDepartureTime()
            ).toMinutes();
            layoverDurations.add((int) layover);
        }
    }

    // Getters
    public List<Flight> getSegments() { return segments; }
    public int getTotalDuration() { return totalDuration; }
    public int getTotalLayoverTime() { return totalLayoverTime; }
    public double getTotalPrice() { return totalPrice; }
    public String getSourceCode() { return sourceCode; }
    public String getDestinationCode() { return destinationCode; }
    public String getSourceCity() { return sourceCity; }
    public String getDestinationCity() { return destinationCity; }
    public int getNumberOfStops() { return numberOfStops; }
    public List<String> getFlightNumbers() { return flightNumbers; }
    public List<String> getAirlines() { return airlines; }
    public List<String> getLayoverAirports() { return layoverAirports; }
    public List<Integer> getLayoverDurations() { return layoverDurations; }

    // Setters
    public void setSegments(List<Flight> segments) { this.segments = segments; }
    public void setTotalDuration(int totalDuration) { this.totalDuration = totalDuration; }
    public void setTotalLayoverTime(int totalLayoverTime) { this.totalLayoverTime = totalLayoverTime; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public void setSourceCity(String sourceCity) { this.sourceCity = sourceCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }
    public void setNumberOfStops(int numberOfStops) { this.numberOfStops = numberOfStops; }
    public void setFlightNumbers(List<String> flightNumbers) { this.flightNumbers = flightNumbers; }
    public void setAirlines(List<String> airlines) { this.airlines = airlines; }
    public void setLayoverAirports(List<String> layoverAirports) { this.layoverAirports = layoverAirports; }
    public void setLayoverDurations(List<Integer> layoverDurations) { this.layoverDurations = layoverDurations; }

}
