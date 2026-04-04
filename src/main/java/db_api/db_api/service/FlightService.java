package db_api.db_api.service;

import db_api.db_api.dto.SeatInfo;
import db_api.db_api.dto.SeatMapDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.*;
import db_api.db_api.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
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

    @Autowired
    private FareClassService fareClassService;

    /**
     * Create a new flight with validation including aircraft time slot conflict check
     */
    @Transactional
    public Flight createFlight(Flight flight) throws BookingException {
        log.info("Creating flight: {}", flight.getFlightNumber());

        // Validate aircraft exists
        if (flight.getAircraft() == null || flight.getAircraft().getId() == null) {
            throw new BookingException("Aircraft ID is required");
        }

        // Check if flight number already exists
        if (flightRepository.existsByFlightNumber(flight.getFlightNumber())) {
            throw new BookingException("Flight number already exists: " + flight.getFlightNumber());
        }

        // Validate source and destination are different
        if (flight.getSourceAirport().getCode().equals(flight.getDestinationAirport().getCode())) {
            throw new BookingException("Source and destination airports cannot be the same");
        }

        // Validate departure time is in future
        if (flight.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BookingException("Departure time must be in the future");
        }

        // Validate arrival time is after departure
        if (flight.getArrivalTime().isBefore(flight.getDepartureTime())) {
            throw new BookingException("Arrival time must be after departure time");
        }

        // ✅ NEW: Check if aircraft is available for the requested time slot
        checkAircraftAvailability(flight.getAircraft().getId(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                null); // null for create (no existing flight ID)

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

        // Initialize sold seats to 0
        flight.setSoldEconomySeats(0);
        flight.setSoldBusinessSeats(0);
        flight.setSoldFirstClassSeats(0);


        flight.setCurrentPriceEconomy(flight.getBasePriceEconomy());
        flight.setCurrentPriceBusiness(flight.getBasePriceBusiness());
        flight.setCurrentPriceFirstClass(flight.getBasePriceFirstClass());

        // Set dynamic pricing multipliers
        flight.setTimeMultiplier(1.0);
        flight.setDemandMultiplier(1.0);
        flight.setDayMultiplier(1.0);
        flight.setFinalPriceMultiplier(1.0);

        // Set status
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setDelayCount(0);
        flight.setTotalDelayMinutes(0);
        flight.setOriginalDepartureTime(flight.getDepartureTime());
        flight.setOriginalArrivalTime(flight.getArrivalTime());

        Flight savedFlight = flightRepository.save(flight);

        // Update dynamic pricing after saving
//        savedFlight.updateAllCurrentPrices();
//        savedFlight = flightRepository.save(savedFlight);

        log.info("✅ Flight created successfully: {}", savedFlight.getFlightNumber());

        return savedFlight;
    }

    /**
     * ✅ NEW: Check if aircraft is available for the requested time slot
     * @param aircraftId Aircraft ID to check
     * @param newDepartureTime Requested departure time
     * @param newArrivalTime Requested arrival time
     * @param excludeFlightId Flight ID to exclude (for update operations, null for create)
     * @throws BookingException if time slot is already booked
     */
    private void checkAircraftAvailability(Long aircraftId,
                                           LocalDateTime newDepartureTime,
                                           LocalDateTime newArrivalTime,
                                           Long excludeFlightId) throws BookingException {

        log.info("Checking aircraft availability for aircraft ID: {} from {} to {}",
                aircraftId, newDepartureTime, newArrivalTime);

        // Get all flights for this aircraft that are SCHEDULED or DELAYED
        List<Flight> conflictingFlights = flightRepository.findByAircraftIdAndStatusIn(
                aircraftId,
                List.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED)
        );

        // If updating an existing flight, exclude the current flight from check
        if (excludeFlightId != null) {
            conflictingFlights = conflictingFlights.stream()
                    .filter(f -> !f.getId().equals(excludeFlightId))
                    .collect(Collectors.toList());
        }

        for (Flight existingFlight : conflictingFlights) {
            if (isTimeSlotOverlapping(newDepartureTime, newArrivalTime,
                    existingFlight.getDepartureTime(),
                    existingFlight.getArrivalTime())) {

                log.warn("Aircraft conflict detected! Existing flight: {} ({}) from {} to {}, " +
                                "New flight: from {} to {}",
                        existingFlight.getFlightNumber(), existingFlight.getId(),
                        existingFlight.getDepartureTime(), existingFlight.getArrivalTime(),
                        newDepartureTime, newArrivalTime);

                throw new BookingException(
                        String.format("Aircraft is already scheduled for a flight!\n" +
                                        "Existing Flight: %s (%s)\n" +
                                        "Departure: %s\n" +
                                        "Arrival: %s\n\n" +
                                        "Please choose a different aircraft or time slot.",
                                existingFlight.getFlightNumber(),
                                existingFlight.getId(),
                                existingFlight.getDepartureTime(),
                                existingFlight.getArrivalTime())
                );
            }
        }

        log.info("Aircraft is available for the requested time slot");
    }

    /**
     * ✅ NEW: Check if two time slots overlap
     * @param start1 Start time of first slot
     * @param end1 End time of first slot
     * @param start2 Start time of second slot
     * @param end2 End time of second slot
     * @return true if time slots overlap
     */
    private static final int MIN_TURNAROUND_MINUTES = 45; // Add at top of class

    private boolean isTimeSlotOverlapping(LocalDateTime newStart, LocalDateTime newEnd,
                                          LocalDateTime existingStart, LocalDateTime existingEnd) {
        // Add turnaround time to existing flight's arrival time
        LocalDateTime aircraftAvailableFrom = existingEnd.plusMinutes(MIN_TURNAROUND_MINUTES);

        // Check if new flight departure is before aircraft is available
        if (newStart.isBefore(aircraftAvailableFrom)) {
            return true; // Conflict - not enough gap
        }

        return false;
    }

    /**
     * Update an existing flight with aircraft availability check
     */
    @Transactional
    public Flight updateFlight(Long flightId, Flight flightDetails) throws BookingException {
        log.info("Updating flight ID: {}", flightId);

        Flight existingFlight = getFlightDetails(flightId);

        // Check if aircraft availability for the new time slot (excluding current flight)
        if (flightDetails.getAircraft() != null && flightDetails.getAircraft().getId() != null) {
            Long newAircraftId = flightDetails.getAircraft().getId();
            LocalDateTime newDepartureTime = flightDetails.getDepartureTime() != null ?
                    flightDetails.getDepartureTime() : existingFlight.getDepartureTime();
            LocalDateTime newArrivalTime = flightDetails.getArrivalTime() != null ?
                    flightDetails.getArrivalTime() : existingFlight.getArrivalTime();

            checkAircraftAvailability(newAircraftId, newDepartureTime, newArrivalTime, flightId);
        }

        // Update fields
        if (flightDetails.getFlightNumber() != null) {
            existingFlight.setFlightNumber(flightDetails.getFlightNumber());
        }
        if (flightDetails.getAircraft() != null && flightDetails.getAircraft().getId() != null) {
            Aircraft aircraft = aircraftRepository.findById(flightDetails.getAircraft().getId())
                    .orElseThrow(() -> new BookingException("Aircraft not found"));
            existingFlight.setAircraft(aircraft);
        }
        if (flightDetails.getDepartureTime() != null) {
            existingFlight.setDepartureTime(flightDetails.getDepartureTime());
        }
        if (flightDetails.getArrivalTime() != null) {
            existingFlight.setArrivalTime(flightDetails.getArrivalTime());
        }
        if (flightDetails.getBasePriceEconomy() != null) {
            existingFlight.setBasePriceEconomy(flightDetails.getBasePriceEconomy());
        }
        if (flightDetails.getBasePriceBusiness() != null) {
            existingFlight.setBasePriceBusiness(flightDetails.getBasePriceBusiness());
        }
        if (flightDetails.getBasePriceFirstClass() != null) {
            existingFlight.setBasePriceFirstClass(flightDetails.getBasePriceFirstClass());
        }

        // Recalculate duration if times changed
        if (flightDetails.getDepartureTime() != null || flightDetails.getArrivalTime() != null) {
            long durationMinutes = java.time.Duration.between(
                    existingFlight.getDepartureTime(),
                    existingFlight.getArrivalTime()
            ).toMinutes();
            existingFlight.setDuration((int) durationMinutes);
        }

        // Update dynamic pricing
        existingFlight.updateAllCurrentPrices();

        Flight savedFlight = flightRepository.save(existingFlight);
        log.info("✅ Flight updated successfully: {}", savedFlight.getFlightNumber());

        return savedFlight;
    }

    /**
     * Get available seats for a flight with pessimistic locking
     */
    @Transactional
    public List<Seat> getAvailableSeats(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);

        // Check if booking is still allowed
        if (!flight.canBook()) {
            throw new BookingException("Booking for this flight is closed. Cutoff time was " +
                    flight.getDepartureTime().minusHours(flight.getBookingCutoffHours()));
        }

        // Use pessimistic lock to prevent race conditions
        List<Seat> allSeats = seatRepository.findByAircraftId(flight.getAircraft().getId());
        List<Long> bookedSeatIds = passengerSeatRepository.findBookedSeatIdsByFlightIdWithLock(flightId);

        return allSeats.stream()
                .filter(seat -> !bookedSeatIds.contains(seat.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get seat map with availability
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
                double flightBasePrice = flight.getCurrentPrice(seat.getSeatClass().name());
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
     * Delay a flight with validation
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

        if (delayMinutes < 1) {
            throw new BookingException("Delay minutes must be positive");
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

        log.info("✈️ Flight {} delayed by {} minutes. Total delays: {}",
                flight.getFlightNumber(), delayMinutes, flight.getDelayCount());

        return flightRepository.save(flight);
    }

    /**
     * Reschedule a flight with aircraft availability check
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

        // ✅ NEW: Check aircraft availability for the new time slot (excluding current flight)
        checkAircraftAvailability(flight.getAircraft().getId(), newDepartureTime, newArrivalTime, flightId);

        if (flight.getOriginalDepartureTime() == null) {
            flight.setOriginalDepartureTime(flight.getDepartureTime());
            flight.setOriginalArrivalTime(flight.getArrivalTime());
        }

        flight.setDepartureTime(newDepartureTime);
        flight.setArrivalTime(newArrivalTime);

        long newDuration = java.time.Duration.between(newDepartureTime, newArrivalTime).toMinutes();
        flight.setDuration((int) newDuration);

        flight.setStatus(FlightStatus.SCHEDULED);

        log.info("✈️ Flight {} rescheduled to {}", flight.getFlightNumber(), newDepartureTime);

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

        log.info("✈️ Flight {} cancelled", flight.getFlightNumber());

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

        log.info("✈️ Flight {} status updated to {}", flight.getFlightNumber(), newStatus);

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
        if (flights.isEmpty()) {
            log.warn("No flights found for airline ID: {}", airlineId);
        }
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

    /**
     * Check if flight is available for booking with dynamic pricing
     */
    public boolean isFlightAvailable(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);
        return flight.getStatus() == FlightStatus.SCHEDULED && flight.canBook();
    }

    /**
     * Update dynamic pricing for a flight
     */
    @Transactional
    public void updateDynamicPricing(Long flightId) throws BookingException {
        Flight flight = getFlightDetails(flightId);
        flight.updateAllCurrentPrices();
        flightRepository.save(flight);
        log.info("Dynamic pricing updated for flight: {}", flight.getFlightNumber());
    }

    /**
     * Update dynamic pricing for all scheduled flights
     */
    @Transactional
    public void updateAllDynamicPricing() {
        List<Flight> flights = flightRepository.findByStatus(FlightStatus.SCHEDULED);
        for (Flight flight : flights) {
            flight.updateAllCurrentPrices();
            flightRepository.save(flight);
        }
        log.info("Dynamic pricing updated for {} flights", flights.size());
    }

    /**
     * Get price breakdown for a flight
     */
    public Map<String, Object> getPriceBreakdown(Long flightId, String seatClass, String fareClassCode) throws BookingException {
        Flight flight = getFlightDetails(flightId);
        FareClass fareClass = fareClassService.getFareClassByCode(fareClassCode);

        double basePrice = getBasePriceForClass(flight, seatClass);
        double fareMultiplier = fareClass.getPriceMultiplier();
        double dynamicPrice = flight.calculateDynamicPrice(seatClass);

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("flightNumber", flight.getFlightNumber());
        breakdown.put("seatClass", seatClass);
        breakdown.put("fareClass", fareClass.getCode());
        breakdown.put("fareClassName", fareClass.getName());
        breakdown.put("basePrice", basePrice);
        breakdown.put("fareClassMultiplier", fareMultiplier);
        breakdown.put("fareClassPrice", Math.round(basePrice * fareMultiplier));
        breakdown.put("dynamicPrice", dynamicPrice);

        // Add price factors
        breakdown.put("timeMultiplier", flight.getTimeMultiplier());
        breakdown.put("demandMultiplier", flight.getDemandMultiplier());
        breakdown.put("dayMultiplier", flight.getDayMultiplier());
        breakdown.put("finalMultiplier", flight.getFinalPriceMultiplier());

        // Baggage allowance
        Map<String, Object> baggage = new HashMap<>();
        baggage.put("cabin", fareClass.getCabinBaggageKg());
        baggage.put("checked", fareClass.getCheckInBaggageKg());
        baggage.put("extraRatePerKg", fareClass.getExtraBaggageRatePerKg());
        breakdown.put("baggageAllowance", baggage);

        // Other benefits
        breakdown.put("mealIncluded", fareClass.getMealIncluded());
        breakdown.put("cancellationFee", fareClass.getCancellationFee());
        breakdown.put("changeFee", fareClass.getChangeFee());
        breakdown.put("seatSelectionFree", fareClass.getSeatSelectionFree());
        breakdown.put("priorityCheckin", fareClass.getPriorityCheckin());
        breakdown.put("priorityBoarding", fareClass.getPriorityBoarding());
        breakdown.put("loungeAccess", fareClass.getLoungeAccess());

        return breakdown;
    }

    private double getBasePriceForClass(Flight flight, String seatClass) {
        switch (seatClass.toUpperCase()) {
            case "BUSINESS":
                return flight.getBasePriceBusiness();
            case "FIRST":
                return flight.getBasePriceFirstClass();
            default:
                return flight.getBasePriceEconomy();
        }
    }
}