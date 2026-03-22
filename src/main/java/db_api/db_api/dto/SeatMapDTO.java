package db_api.db_api.dto;

import db_api.db_api.enums.SeatType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SeatMapDTO {
    private Long flightId;
    private String flightNumber;
    private boolean canBook;
    private LocalDateTime bookingCutoffTime;
    private List<SeatInfo> economySeats = new ArrayList<>();
    private List<SeatInfo> businessSeats = new ArrayList<>();
    private List<SeatInfo> firstClassSeats = new ArrayList<>();
}

