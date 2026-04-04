package db_api.db_api.dto;

import db_api.db_api.model.Seat;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SeatWithRowInfo {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private String seatType;
    private Boolean hasExtraLegroom;
    private Boolean isNearExit;
    private Double extraPrice;
    private Boolean isActive;
    private int rowNumber;
    private String columnLetter;
    private String seatCategory;
    private String seatTypeClass;
    private int premiumPercent;
    private boolean isAvailable; // Will be set by booking service

    public SeatWithRowInfo(Seat seat) {
        this.id = seat.getId();
        this.seatNumber = seat.getSeatNumber();
        this.seatClass = seat.getSeatClass() != null ? seat.getSeatClass().name() : null;
        this.seatType = seat.getSeatType() != null ? seat.getSeatType().name() : null;
        this.hasExtraLegroom = seat.getHasExtraLegroom();
        this.isNearExit = seat.getIsNearExit();
        this.extraPrice = seat.getExtraPrice();
        this.isActive = seat.getIsActive();
        this.rowNumber = seat.getRowNumber();
        this.columnLetter = seat.getColumnLetter();
        this.seatCategory = seat.getSeatCategory();
        this.seatTypeClass = seat.getSeatTypeClass();
        this.premiumPercent = seat.getPremiumPercent();
        this.isAvailable = true; // default
    }
}