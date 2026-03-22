package db_api.db_api.dto;

import db_api.db_api.enums.SeatType;
import lombok.Data;

@Data
public class SeatInfo {
    private Long seatId;
    private String seatNumber;
    private SeatType seatType;
    private Boolean hasExtraLegroom;
    private Boolean isNearExit;
    private Double extraPrice;
    private Boolean isAvailable;
    private Double price;
}