package db_api.db_api.service;

import db_api.db_api.dto.ConnectingFlightDTO;
import db_api.db_api.dto.SeatInfo;
import db_api.db_api.dto.SeatMapDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.*;
import db_api.db_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PassengerSeatRepository passengerSeatRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private BookingFlightRepository bookingFlightRepository;

    /**
     * Create a new flight
     */
    @Transactional
    public Flight createFlight(Flight flight) throws BookingException {
        // Validate aircraft exists
        if (flight.getAircraft() == null || flight.getAircraft().getId() == null) {
            throw new BookingException("Aircraft ID is required");
        }

        // Check if flight number already exists
        if (flightRepository.existsByFlightNumber(flight.getFlightNumber())) {
            throw new BookingException("Flight number already exists: " + flight.getFlightNumber());
        }

        // Validate departure time is in future
        if (flight.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BookingException("Departure time must be in the future");
        }

        // Validate arrival time is after departure
        if (flight.getArrivalTime().isBefore(flight.getDepartureTime())) {
            throw new BookingException("Arrival time must be after departure time");
        }

        // Calculate duration
        long durationMinutes = java.time.Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        ).toMinutes();
        flight.setDuration((int) durationMinutes);

        // Fetch and set source airport
        Airport sourceAirport = airportRepository.findByCode(flight.getSourceAirport().getCode())
                .orElseThrow(() -> new BookingException("Source airport not found with code: " + flight.getSourceAirport().getCode()));
        flight.setSourceAirport(sourceAirport);

        // Fetch and set destination airport
        Airport destAirport = airportRepository.findByCode(flight.getDestinationAirport().getCode())
                .orElseThrow(() -> new BookingException("Destination airport not found with code: " + flight.getDestinationAirport().getCode()));
        flight.setDestinationAirport(destAirport);

        // Fetch and set aircraft
        Aircraft aircraft = aircraftRepository.findById(flight.getAircraft().getId())
                .orElseThrow(() -> new BookingException("Aircraft not found with ID: " + flight.getAircraft().getId()));
        flight.setAircraft(aircraft);

        // Set available seats from aircraft
        flight.setAvailableEconomySeats(aircraft.getEconomySeats());
        flight.setAvailableBusinessSeats(aircraft.getBusinessSeats());
        flight.setAvailableFirstClassSeats(aircraft.getFirstClassSeats());

        // Set current prices
        flight.setCurrentPriceEconomy(flight.getBasePriceEconomy());
        flight.setCurrentPriceBusiness(flight.getBasePriceBusiness());
        flight.setCurrentPriceFirstClass(flight.getBasePriceFirstClass());

        // Set status
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setDelayCount(0);
        flight.setTotalDelayMinutes(0);
        flight.setOriginalDepartureTime(flight.getDepartureTime());
        flight.setOriginalArrivalTime(flight.getArrivalTime());

        return flightRepository.save(flight);
    }

    /**
     * Get available seats for a flight
     */
    public List<Seat> getAvailableSeats(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        // Check if booking is still allowed
        if (!flight.canBook()) {
            throw new BookingException("Booking for this flight is closed. Cutoff time was " +
                    flight.getDepartureTime().minusHours(flight.getBookingCutoffHours()));
        }

        List<Seat> allSeats = seatRepository.findByAircraftId(flight.getAircraft().getId());
        List<Long> bookedSeatIds = passengerSeatRepository.findBookedSeatIdsByFlightId(flightId);

        return allSeats.stream()
                .filter(seat -> !bookedSeatIds.contains(seat.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get seat map with availability and prices calculated from flight base price
     */
    public SeatMapDTO getSeatMap(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);
        List<Seat> allSeats = seatRepository.findByAircraftId(flight.getAircraft().getId());
        List<Long> bookedSeatIds = passengerSeatRepository.findBookedSeatIdsByFlightId(flightId);

        SeatMapDTO seatMap = new SeatMapDTO();
        seatMap.setFlightId(flightId);
        seatMap.setFlightNumber(flight.getFlightNumber());
        seatMap.setCanBook(flight.canBook());
        seatMap.setBookingCutoffTime(flight.getDepartureTime().minusHours(flight.getBookingCutoffHours()));

        for (Seat seat : allSeats) {
            SeatInfo seatInfo = new SeatInfo();
            seatInfo.setSeatId(seat.getId());
            seatInfo.setSeatNumber(seat.getSeatNumber());
            seatInfo.setSeatType(seat.getSeatType());
            seatInfo.setHasExtraLegroom(seat.getHasExtraLegroom());
            seatInfo.setIsNearExit(seat.getIsNearExit());
            seatInfo.setExtraPrice(seat.getExtraPrice());
            seatInfo.setIsAvailable(!bookedSeatIds.contains(seat.getId()));

            if (seatInfo.getIsAvailable()) {
                // Get base price from FLIGHT based on seat class
                double flightBasePrice;
                switch (seat.getSeatClass()) {
                    case BUSINESS:
                        flightBasePrice = flight.getCurrentPrice("BUSINESS");
                        break;
                    case FIRST:
                        flightBasePrice = flight.getCurrentPrice("FIRST");
                        break;
                    default:
                        flightBasePrice = flight.getCurrentPrice("ECONOMY");
                }

                // Calculate final price using seat's calculateFinalPrice method
                seatInfo.setPrice(seat.calculateFinalPrice(flightBasePrice));
            }

            switch (seat.getSeatClass()) {
                case ECONOMY:
                    seatMap.getEconomySeats().add(seatInfo);
                    break;
                case BUSINESS:
                    seatMap.getBusinessSeats().add(seatInfo);
                    break;
                case FIRST:
                    seatMap.getFirstClassSeats().add(seatInfo);
                    break;
            }
        }

        return seatMap;
    }

    /**
     * Delay a flight - can be done multiple times
     */
    @Transactional
    public Flight delayFlight(Long flightId, int delayMinutes) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        if (flight.getStatus() != FlightStatus.SCHEDULED) {
            throw new BookingException("Only scheduled flights can be delayed");
        }

        if (delayMinutes > 360) {
            throw new BookingException("Cannot delay flight by more than 6 hours");
        }

        flight.setDepartureTime(flight.getDepartureTime().plusMinutes(delayMinutes));
        flight.setArrivalTime(flight.getArrivalTime().plusMinutes(delayMinutes));

        long newDuration = java.time.Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        ).toMinutes();
        flight.setDuration((int) newDuration);

        flight.setDelayCount(flight.getDelayCount() + 1);
        flight.setTotalDelayMinutes(flight.getTotalDelayMinutes() + delayMinutes);
        flight.setStatus(FlightStatus.DELAYED);

        return flightRepository.save(flight);
    }

    /**
     * Reschedule a flight
     */
    @Transactional
    public Flight rescheduleFlight(Long flightId, LocalDateTime newDepartureTime, LocalDateTime newArrivalTime)
            throws BookingException {
        Flight flight = getFlightDetails(flightId);

        if (flight.getStatus() == FlightStatus.CANCELLED || flight.getStatus() == FlightStatus.COMPLETED) {
            throw new BookingException("Cannot reschedule cancelled or completed flight");
        }

        if (newDepartureTime.isBefore(LocalDateTime.now())) {
            throw new BookingException("New departure time must be in the future");
        }

        if (newArrivalTime.isBefore(newDepartureTime)) {
            throw new BookingException("Arrival time must be after departure time");
        }

        if (flight.getOriginalDepartureTime() == null) {
            flight.setOriginalDepartureTime(flight.getDepartureTime());
            flight.setOriginalArrivalTime(flight.getArrivalTime());
        }

        flight.setDepartureTime(newDepartureTime);
        flight.setArrivalTime(newArrivalTime);

        long newDuration = java.time.Duration.between(newDepartureTime, newArrivalTime).toMinutes();
        flight.setDuration((int) newDuration);

        flight.setStatus(FlightStatus.SCHEDULED);

        return flightRepository.save(flight);
    }

    /**
     * Cancel a flight
     */
    @Transactional
    public Flight cancelFlight(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new BookingException("Flight is already cancelled");
        }

        if (flight.getStatus() == FlightStatus.COMPLETED) {
            throw new BookingException("Cannot cancel completed flight");
        }

        flight.setStatus(FlightStatus.CANCELLED);
        return flightRepository.save(flight);
    }

    /**
     * Update flight status
     */
    public Flight updateFlightStatus(Long flightId, FlightStatus newStatus) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new BookingException("Cannot change status of cancelled flight");
        }

        if (flight.getStatus() == FlightStatus.COMPLETED) {
            throw new BookingException("Cannot change status of completed flight");
        }

        flight.setStatus(newStatus);
        return flightRepository.save(flight);
    }

    /**
     * Get flight details
     */
    public Flight getFlightDetails(Long flightId) throws BookingException {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found with ID: " + flightId));
    }

    /**
     * Get flights by airline
     */
    public List<Flight> getFlightsByAirline(Long airlineId) throws BookingException {
        List<Flight> flights = flightRepository.findByAircraftAirlineId(airlineId);
        return flights;
    }

    /**
     * Get all flights
     */
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    /**
     * Get flights by status
     */
    public List<Flight> getFlightsByStatus(FlightStatus status) {
        return flightRepository.findByStatus(status);
    }
}