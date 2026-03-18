package db_api.db_api.dto;

import java.util.ArrayList;
import java.util.List;

public class SeatMapDTO {
    private Long flightId;
    private String flightNumber;
    private List<SeatInfo> economySeats = new ArrayList<>();
    private List<SeatInfo> businessSeats = new ArrayList<>();
    private List<SeatInfo> firstClassSeats = new ArrayList<>();

    // Getters and Setters
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public List<SeatInfo> getEconomySeats() { return economySeats; }
    public void setEconomySeats(List<SeatInfo> economySeats) { this.economySeats = economySeats; }

    public List<SeatInfo> getBusinessSeats() { return businessSeats; }
    public void setBusinessSeats(List<SeatInfo> businessSeats) { this.businessSeats = businessSeats; }

    public List<SeatInfo> getFirstClassSeats() { return firstClassSeats; }
    public void setFirstClassSeats(List<SeatInfo> firstClassSeats) { this.firstClassSeats = firstClassSeats; }
}
