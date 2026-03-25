package db_api.db_api.dto;

import db_api.db_api.model.Flight;
import lombok.Data;

import java.util.List;

@Data
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

    // ✅ NEW: Hub & Spoke fields
    private String connectionType; // "HUB", "NORMAL"
    private String hubAirport; // Which hub was used
    private boolean sameAirline; // Whether all segments are same airline
    private boolean protectedConnection; // Whether airline protects the connection

    public ConnectingFlightDTO(List<Flight> segments) {
        this.segments = segments;
        this.numberOfStops = segments.size() - 1;
        this.sameAirline = checkSameAirline(segments);
        this.protectedConnection = this.sameAirline; // Same airline = protected
        calculateTotals();
        extractFlightNumbers();
        extractAirlines();
        extractLayoverInfo();
    }

    private boolean checkSameAirline(List<Flight> segments) {
        if (segments.isEmpty()) return false;
        Long firstAirlineId = segments.get(0).getAircraft().getAirline().getId();
        for (Flight flight : segments) {
            if (!flight.getAircraft().getAirline().getId().equals(firstAirlineId)) {
                return false;
            }
        }
        return true;
    }

    private void calculateTotals() {
        this.totalDuration = 0;
        this.totalLayoverTime = 0;
        this.totalPrice = 0;

        for (int i = 0; i < segments.size(); i++) {
            Flight flight = segments.get(i);
            totalDuration += flight.getDuration();
            totalPrice += flight.getBasePriceEconomy();

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

        // ✅ NEW: Discount for hub connections
        if ("HUB".equals(connectionType)) {
            totalPrice = totalPrice * 0.95; // 5% discount for hub connections
        }

        // ✅ NEW: Discount for same airline connections
        if (sameAirline) {
            totalPrice = totalPrice * 0.97; // Additional 3% discount for same airline
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
}