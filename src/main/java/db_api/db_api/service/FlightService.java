package db_api.db_api.service;


import db_api.db_api.dto.ConnectingFlightDTO;
import db_api.db_api.dto.SeatInfo;
import db_api.db_api.dto.SeatMapDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.enums.SeatClass;
import db_api.db_api.enums.SeatType;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Airport;
import db_api.db_api.model.Flight;
import db_api.db_api.model.Seat;
import db_api.db_api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

    // FlightService.java ke starting mein ye add karo
    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PassengerSeatRepository passengerSeatRepository;

    @Autowired
    private AirportRepository airportRepository;  // ✅ YEH ADD KARO


    /**
     * Get available seats for a specific flight
     */
    public List<Seat> getAvailableSeats(Long flightId) throws BookingException {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found with ID: " + flightId));

        // Check if flight is scheduled
        if (flight.getStatus() != FlightStatus.SCHEDULED) {
            throw new BookingException("Flight is not available for booking. Current status: " + flight.getStatus());
        }

        // Get all seats for this aircraft
        List<Seat> allSeats = seatRepository.findByAircraftId(flight.getAircraft().getId());

        // Get booked seat IDs for this flight
        List<Long> bookedSeatIds = passengerSeatRepository.findBookedSeatIdsByFlightId(flightId);

        // Filter available seats (not booked)
        return allSeats.stream()
                .filter(seat -> !bookedSeatIds.contains(seat.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get available seats by class for a specific flight
     */
    public List<Seat> getAvailableSeatsByClass(Long flightId, SeatClass seatClass) throws BookingException {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found with ID: " + flightId));

        List<Seat> availableSeats = getAvailableSeats(flightId);

        return availableSeats.stream()
                .filter(seat -> seat.getSeatClass() == seatClass)
                .collect(Collectors.toList());
    }

    /**
     * Get flight details by ID
     */
    public Flight getFlightDetails(Long flightId) throws BookingException {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new BookingException("Flight not found with ID: " + flightId));
    }

    /**
     * Get all flights by airline
     */
    public List<Flight> getFlightsByAirline(Long airlineId) throws BookingException {
        List<Flight> flights = flightRepository.findByAircraftAirlineId(airlineId);

        if (flights.isEmpty()) {
            throw new BookingException("No flights found for airline ID: " + airlineId);
        }

        return flights;
    }

    /**
     * Get flights by status
     */
    public List<Flight> getFlightsByStatus(FlightStatus status) {
        return flightRepository.findByStatus(status);
    }

    /**
     * Update flight status (for airline dashboard)
     */
    public Flight updateFlightStatus(Long flightId, FlightStatus newStatus) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        // Validate status transition
        validateStatusTransition(flight.getStatus(), newStatus);

        flight.setStatus(newStatus);
        return flightRepository.save(flight);
    }

    /**
     * Validate if status transition is allowed
     */
    private void validateStatusTransition(FlightStatus currentStatus, FlightStatus newStatus)
            throws BookingException {

        if (currentStatus == FlightStatus.CANCELLED) {
            throw new BookingException("Cannot change status of cancelled flight");
        }

        if (currentStatus == FlightStatus.COMPLETED) {
            throw new BookingException("Cannot change status of completed flight");
        }

        if (currentStatus == newStatus) {
            throw new BookingException("Flight is already in " + currentStatus + " status");
        }

        // Add more business rules as needed
        if (currentStatus == FlightStatus.SCHEDULED && newStatus == FlightStatus.COMPLETED) {
            throw new BookingException("Flight cannot be marked as completed before departure");
        }
    }

    /**
     * Delay a flight
     */
    public Flight delayFlight(Long flightId, int delayMinutes) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        if (flight.getStatus() != FlightStatus.SCHEDULED) {
            throw new BookingException("Only scheduled flights can be delayed");
        }

        // Update arrival and departure times
        flight.setDepartureTime(flight.getDepartureTime().plusMinutes(delayMinutes));
        flight.setArrivalTime(flight.getArrivalTime().plusMinutes(delayMinutes));
        flight.setStatus(FlightStatus.DELAYED);

        // Update duration if needed
        long newDuration = java.time.Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        ).toMinutes();
        flight.setDuration((int) newDuration);

        return flightRepository.save(flight);
    }

    /**
     * Get seat map for a flight (organized by class and position)
     */
    public SeatMapDTO getSeatMap(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);
        List<Seat> allSeats = seatRepository.findByAircraftId(flight.getAircraft().getId());
        List<Long> bookedSeatIds = passengerSeatRepository.findBookedSeatIdsByFlightId(flightId);

        SeatMapDTO seatMap = new SeatMapDTO();
        seatMap.setFlightId(flightId);
        seatMap.setFlightNumber(flight.getFlightNumber());

        // Organize seats by class
        for (Seat seat : allSeats) {
            SeatInfo seatInfo = new SeatInfo();
            seatInfo.setSeatId(seat.getId());
            seatInfo.setSeatNumber(seat.getSeatNumber());
            seatInfo.setSeatType(seat.getSeatType());
            seatInfo.setHasExtraLegroom(seat.getHasExtraLegroom());
            seatInfo.setIsNearExit(seat.getIsNearExit());
            seatInfo.setExtraPrice(seat.getExtraPrice());
            seatInfo.setIsAvailable(!bookedSeatIds.contains(seat.getId()));

            // Calculate price for this seat on this flight
            if (seatInfo.getIsAvailable()) {
                seatInfo.setPrice(calculateSeatPrice(flight, seat));
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
     * Calculate seat price (reuse from BookingService)
     */
    private double calculateSeatPrice(Flight flight, Seat seat) {
        double basePrice;

        switch (seat.getSeatClass()) {
            case BUSINESS:
                basePrice = flight.getBasePriceBusiness();
                break;
            case FIRST:
                basePrice = flight.getBasePriceFirstClass();
                break;
            default:
                basePrice = flight.getBasePriceEconomy();
        }

        // Add extras
        if (seat.getSeatType() == SeatType.WINDOW) {
            basePrice += 500;
        }
        if (seat.getHasExtraLegroom()) {
            basePrice += 1000;
        }
        if (seat.getIsNearExit()) {
            basePrice += 750;
        }
        basePrice += seat.getExtraPrice() != null ? seat.getExtraPrice() : 0;

        return basePrice;
    }

    // FlightSearchService.java mein findConnectingFlights method update karo:

    private List<ConnectingFlightDTO> findConnectingFlights(Airport source, Airport destination,
                                                            LocalDateTime startOfDay, LocalDateTime endOfDay) {
        List<ConnectingFlightDTO> connectingFlights = new ArrayList<>();

        // Find flights from source to intermediate airports
        List<Flight> firstLegFlights = flightRepository
                .findBySourceAirportCodeAndDepartureTimeBetween(source.getCode(), startOfDay, endOfDay);

        for (Flight firstLeg : firstLegFlights) {
            // Skip if first leg itself goes to destination
            if (firstLeg.getDestinationAirport().getCode().equals(destination.getCode())) {
                continue;
            }

            // Find connecting flights from first leg's destination to our destination
            LocalDateTime minConnectionTime = firstLeg.getArrivalTime().plusHours(1);
            LocalDateTime maxConnectionTime = firstLeg.getArrivalTime().plusHours(6);

            List<Flight> secondLegFlights = flightRepository
                    .findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
                            firstLeg.getDestinationAirport().getCode(),
                            destination.getCode(),
                            FlightStatus.SCHEDULED);

            for (Flight secondLeg : secondLegFlights) {
                if (!secondLeg.getDepartureTime().isBefore(minConnectionTime) &&
                        !secondLeg.getDepartureTime().isAfter(maxConnectionTime)) {

                    // Create connecting flight with two segments
                    List<Flight> segments = new ArrayList<>();
                    segments.add(firstLeg);
                    segments.add(secondLeg);

                    ConnectingFlightDTO connectingFlight = new ConnectingFlightDTO(segments);
                    connectingFlights.add(connectingFlight);
                }
            }

            // Also look for three-segment connections (optional)
            findThreeSegmentConnections(firstLeg, destination, connectingFlights);
        }

        return connectingFlights;
    }

    private void findThreeSegmentConnections(Flight firstLeg, Airport destination,
                                             List<ConnectingFlightDTO> connectingFlights) {
        // Find second leg to intermediate airport
        List<Flight> secondLegFlights = flightRepository
                .findBySourceAirportCodeAndDepartureTimeBetween(
                        firstLeg.getDestinationAirport().getCode(),
                        firstLeg.getArrivalTime().plusHours(1),
                        firstLeg.getArrivalTime().plusHours(4));

        for (Flight secondLeg : secondLegFlights) {
            // Skip if second leg goes to destination
            if (secondLeg.getDestinationAirport().getCode().equals(destination.getCode())) {
                continue;
            }

            // Find third leg to destination
            LocalDateTime minConnectionTime = secondLeg.getArrivalTime().plusHours(1);
            LocalDateTime maxConnectionTime = secondLeg.getArrivalTime().plusHours(4);

            List<Flight> thirdLegFlights = flightRepository
                    .findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
                            secondLeg.getDestinationAirport().getCode(),
                            destination.getCode(),
                            FlightStatus.SCHEDULED);

            for (Flight thirdLeg : thirdLegFlights) {
                if (!thirdLeg.getDepartureTime().isBefore(minConnectionTime) &&
                        !thirdLeg.getDepartureTime().isAfter(maxConnectionTime)) {

                    List<Flight> segments = new ArrayList<>();
                    segments.add(firstLeg);
                    segments.add(secondLeg);
                    segments.add(thirdLeg);

                    ConnectingFlightDTO connectingFlight = new ConnectingFlightDTO(segments);
                    connectingFlights.add(connectingFlight);
                }
            }
        }


    }


    // FlightService.java mein ye method add karo (already tumne add kiya tha)
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    // FlightService.java - createFlight method update karo

    public Flight createFlight(Flight flight) throws BookingException {
        // Validate aircraft exists
        if (flight.getAircraft() == null || flight.getAircraft().getId() == null) {
            throw new BookingException("Aircraft ID is required");
        }

        // Check if flight number already exists
        if (flightRepository.existsByFlightNumber(flight.getFlightNumber())) {
            throw new BookingException("Flight number already exists: " + flight.getFlightNumber());
        }

        // ✅ Fetch and set source airport from database using code
        if (flight.getSourceAirport() == null || flight.getSourceAirport().getCode() == null) {
            throw new BookingException("Source airport code is required");
        }
        Airport sourceAirport = airportRepository.findByCode(flight.getSourceAirport().getCode())
                .orElseThrow(() -> new BookingException("Source airport not found with code: " + flight.getSourceAirport().getCode()));
        flight.setSourceAirport(sourceAirport);

        // ✅ Fetch and set destination airport from database using code
        if (flight.getDestinationAirport() == null || flight.getDestinationAirport().getCode() == null) {
            throw new BookingException("Destination airport code is required");
        }
        Airport destAirport = airportRepository.findByCode(flight.getDestinationAirport().getCode())
                .orElseThrow(() -> new BookingException("Destination airport not found with code: " + flight.getDestinationAirport().getCode()));
        flight.setDestinationAirport(destAirport);

        // Set available seats from aircraft
        Aircraft aircraft = aircraftRepository.findById(flight.getAircraft().getId())
                .orElseThrow(() -> new BookingException("Aircraft not found with ID: " + flight.getAircraft().getId()));
        flight.setAircraft(aircraft);

        flight.setAvailableEconomySeats(aircraft.getEconomySeats());
        flight.setAvailableBusinessSeats(aircraft.getBusinessSeats());
        flight.setAvailableFirstClassSeats(aircraft.getFirstClassSeats());

        return flightRepository.save(flight);
    }
}
