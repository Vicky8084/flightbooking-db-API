package db_api.db_api.dto;

import db_api.db_api.model.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class BookingResponseDTO {
    private Long id;
    private String pnrNumber;
    private LocalDateTime bookingTime;
    private Double totalAmount;
    private String status;
    private String fareClassCode;
    private List<BookingFlightDTO> bookingFlights = new ArrayList<>();
    private List<PassengerInfoDTO> passengers = new ArrayList<>();
    private PaymentInfoDTO payment;

    public BookingResponseDTO(Booking booking) {
        this.id = booking.getId();
        this.pnrNumber = booking.getPnrNumber();
        this.bookingTime = booking.getBookingTime();
        this.totalAmount = booking.getTotalAmount();
        this.status = booking.getStatus() != null ? booking.getStatus().name() : null;
        this.fareClassCode = booking.getFareClassCode();

        if (booking.getBookingFlights() != null) {
            this.bookingFlights = booking.getBookingFlights().stream()
                    .map(BookingFlightDTO::new)
                    .collect(Collectors.toList());
        }

        if (booking.getPassengers() != null) {
            this.passengers = booking.getPassengers().stream()
                    .map(PassengerInfoDTO::new)
                    .collect(Collectors.toList());
        }

        if (booking.getPayment() != null) {
            this.payment = new PaymentInfoDTO(booking.getPayment());
        }
    }

    @Data
    public static class BookingFlightDTO {
        private Long id;
        private Integer flightSequence;
        private FlightInfoDTO flight;
        private List<PassengerSeatInfoDTO> passengerSeats = new ArrayList<>();

        public BookingFlightDTO(BookingFlight bf) {
            this.id = bf.getId();
            this.flightSequence = bf.getFlightSequence();
            if (bf.getFlight() != null) {
                this.flight = new FlightInfoDTO(bf.getFlight());
            }
            if (bf.getPassengerSeats() != null) {
                this.passengerSeats = bf.getPassengerSeats().stream()
                        .map(PassengerSeatInfoDTO::new)
                        .collect(Collectors.toList());
            }
        }
    }

    @Data
    public static class FlightInfoDTO {
        private Long id;
        private String flightNumber;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private Integer duration;
        private String status;
        private AirportInfoDTO sourceAirport;
        private AirportInfoDTO destinationAirport;
        private AircraftInfoDTO aircraft;

        public FlightInfoDTO(Flight flight) {
            this.id = flight.getId();
            this.flightNumber = flight.getFlightNumber();
            this.departureTime = flight.getDepartureTime();
            this.arrivalTime = flight.getArrivalTime();
            this.duration = flight.getDuration();
            this.status = flight.getStatus() != null ? flight.getStatus().name() : null;

            if (flight.getSourceAirport() != null) {
                this.sourceAirport = new AirportInfoDTO(flight.getSourceAirport());
            }
            if (flight.getDestinationAirport() != null) {
                this.destinationAirport = new AirportInfoDTO(flight.getDestinationAirport());
            }
            if (flight.getAircraft() != null) {
                this.aircraft = new AircraftInfoDTO(flight.getAircraft());
            }
        }
    }

    @Data
    public static class AirportInfoDTO {
        private String code;
        private String city;
        private String name;

        public AirportInfoDTO(Airport airport) {
            this.code = airport.getCode();
            this.city = airport.getCity();
            this.name = airport.getName();
        }
    }

    @Data
    public static class AircraftInfoDTO {
        private Long id;
        private String model;
        private AirlineInfoDTO airline;

        public AircraftInfoDTO(Aircraft aircraft) {
            this.id = aircraft.getId();
            this.model = aircraft.getModel();
            if (aircraft.getAirline() != null) {
                this.airline = new AirlineInfoDTO(aircraft.getAirline());
            }
        }
    }

    @Data
    public static class AirlineInfoDTO {
        private Long id;
        private String name;
        private String code;

        public AirlineInfoDTO(Airline airline) {
            this.id = airline.getId();
            this.name = airline.getName();
            this.code = airline.getCode();
        }
    }

    @Data
    public static class PassengerSeatInfoDTO {
        private Long id;
        private Double seatPrice;
        private PassengerInfoDTO passenger;
        private SeatInfoDTO seat;

        public PassengerSeatInfoDTO(PassengerSeat ps) {
            this.id = ps.getId();
            this.seatPrice = ps.getSeatPrice();
            if (ps.getPassenger() != null) {
                this.passenger = new PassengerInfoDTO(ps.getPassenger());
            }
            if (ps.getSeat() != null) {
                this.seat = new SeatInfoDTO(ps.getSeat());
            }
        }
    }

    @Data
    public static class PassengerInfoDTO {
        private Long id;
        private String fullName;
        private Integer age;
        private String gender;
        private String email;
        private String phoneNumber;

        public PassengerInfoDTO(Passenger passenger) {
            this.id = passenger.getId();
            this.fullName = passenger.getFullName();
            this.age = passenger.getAge();
            this.gender = passenger.getGender();
            this.email = passenger.getEmail();
            this.phoneNumber = passenger.getPhoneNumber();
        }
    }

    @Data
    public static class SeatInfoDTO {
        private Long id;
        private String seatNumber;

        public SeatInfoDTO(Seat seat) {
            this.id = seat.getId();
            this.seatNumber = seat.getSeatNumber();
        }
    }

    @Data
    public static class PaymentInfoDTO {
        private String paymentMethod;
        private String transactionId;
        private String status;

        public PaymentInfoDTO(Payment payment) {
            this.paymentMethod = payment.getPaymentMethod();
            this.transactionId = payment.getTransactionId();
            this.status = payment.getStatus() != null ? payment.getStatus().name() : null;
        }
    }
}