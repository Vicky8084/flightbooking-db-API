package db_api.db_api.service;

import db_api.db_api.dto.BookingRequestDTO;
import db_api.db_api.dto.FlightBookingDTO;
import db_api.db_api.dto.PassengerDTO;
import db_api.db_api.dto.PassengerSeatDTO;
import db_api.db_api.enums.BookingStatus;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.enums.PaymentStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.*;
import db_api.db_api.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingFlightRepository bookingFlightRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private PassengerSeatRepository passengerSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FareClassRepository fareClassRepository;

    @Transactional
    public Booking createBooking(BookingRequestDTO request) throws BookingException {
        log.info("Creating booking for user ID: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BookingException("User not found"));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPnrNumber(generatePNR());
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        if (request.getFareClassCode() != null) {
            FareClass fareClass = fareClassRepository.findByCode(request.getFareClassCode())
                    .orElseThrow(() -> new BookingException("Fare class not found: " + request.getFareClassCode()));
            booking.setFareClass(fareClass);
            booking.setFareClassCode(request.getFareClassCode());
        }

        Booking savedBooking = bookingRepository.save(booking);

        List<Passenger> passengers = new ArrayList<>();
        double totalAmount = 0.0;

        // ✅ IMPORTANT: Get flights list
        List<FlightBookingDTO> flightsToProcess = request.getFlights();

        // ✅ LOG the raw request for debugging
        log.info("Full request received: userId={}, flights={}, fareClassCode={}",
                request.getUserId(), flightsToProcess, request.getFareClassCode());

        // If flights list is null or empty, try single flight mode
        if (flightsToProcess == null || flightsToProcess.isEmpty()) {
            if (request.getFlightId() != null) {
                log.info("Using single flight mode for flight ID: {}", request.getFlightId());
                FlightBookingDTO singleFlight = new FlightBookingDTO();
                singleFlight.setFlightId(request.getFlightId());
                singleFlight.setSequence(1);
                // For single flight, we need passengerSeats from somewhere
                // This is a fallback - ideally should not happen
                flightsToProcess = List.of(singleFlight);
            } else {
                throw new BookingException("No flights specified for booking");
            }
        }

        log.info("Processing {} flights", flightsToProcess.size());

        for (FlightBookingDTO flightDTO : flightsToProcess) {
            log.info("Processing flight DTO: {}", flightDTO);

            if (flightDTO.getFlightId() == null) {
                log.error("Flight ID is null in booking request! DTO: {}", flightDTO);
                throw new BookingException("Flight ID cannot be null");
            }

            Flight flight = flightRepository.findById(flightDTO.getFlightId())
                    .orElseThrow(() -> new BookingException("Flight not found: " + flightDTO.getFlightId()));

            if (!flight.canBook()) {
                throw new BookingException("Booking for flight " + flight.getFlightNumber() +
                        " is closed. Cutoff time was: " +
                        flight.getDepartureTime().minusHours(flight.getBookingCutoffHours()));
            }

            if (flight.getStatus() != FlightStatus.SCHEDULED) {
                throw new BookingException("Flight is not available for booking: " + flight.getFlightNumber());
            }

            BookingFlight bookingFlight = new BookingFlight();
            bookingFlight.setBooking(savedBooking);
            bookingFlight.setFlight(flight);
            bookingFlight.setFlightSequence(flightDTO.getSequence() != null ? flightDTO.getSequence() : 1);

            BookingFlight savedBookingFlight = bookingFlightRepository.save(bookingFlight);

            List<PassengerSeatDTO> passengerSeats = flightDTO.getPassengerSeats();
            if (passengerSeats == null || passengerSeats.isEmpty()) {
                throw new BookingException("No seats selected for flight: " + flight.getFlightNumber());
            }

            for (PassengerSeatDTO psDTO : passengerSeats) {
                int passengerIndex = psDTO.getPassengerIndex();

                Passenger passenger;
                if (passengerIndex < passengers.size()) {
                    passenger = passengers.get(passengerIndex);
                } else {
                    PassengerDTO pDTO = request.getPassengers().get(passengerIndex);
                    passenger = new Passenger();
                    passenger.setFullName(pDTO.getFullName());
                    passenger.setAge(pDTO.getAge());
                    passenger.setGender(pDTO.getGender());
                    passenger.setPassportNumber(pDTO.getPassportNumber());
                    passenger.setNationality(pDTO.getNationality());
                    passenger.setBooking(savedBooking);

                    if (pDTO.getEmail() != null && !pDTO.getEmail().isEmpty()) {
                        passenger.setEmail(pDTO.getEmail());
                        log.info("Setting email for passenger: {}", pDTO.getEmail());
                    }
                    if (pDTO.getPhoneNumber() != null && !pDTO.getPhoneNumber().isEmpty()) {
                        passenger.setPhoneNumber(pDTO.getPhoneNumber());
                        log.info("Setting phone for passenger: {}", pDTO.getPhoneNumber());
                    }

                    passenger = passengerRepository.save(passenger);
                    passengers.add(passenger);
                    log.info("✅ Passenger saved with ID: {}, Email: {}", passenger.getId(), passenger.getEmail());
                }

                Seat seat = seatRepository.findById(psDTO.getSeatId())
                        .orElseThrow(() -> new BookingException("Seat not found: " + psDTO.getSeatId()));

                if (!seat.getAircraft().getId().equals(flight.getAircraft().getId())) {
                    throw new BookingException("Seat does not belong to this aircraft");
                }

                if (passengerSeatRepository.isSeatBookedForFlight(seat.getId(), flight.getId())) {
                    throw new BookingException("Seat " + seat.getSeatNumber() + " is already booked for flight " + flight.getFlightNumber());
                }

                double flightBasePrice = flight.getCurrentPrice(seat.getSeatClass().name());
                double seatPrice = seat.calculateFinalPrice(flightBasePrice);
                totalAmount += seatPrice;

                PassengerSeat passengerSeat = new PassengerSeat();
                passengerSeat.setBookingFlight(savedBookingFlight);
                passengerSeat.setPassenger(passenger);
                passengerSeat.setSeat(seat);
                passengerSeat.setSeatPrice(seatPrice);

                passengerSeatRepository.save(passengerSeat);

                log.info("Seat {} assigned to passenger {} for flight {}",
                        seat.getSeatNumber(), passenger.getFullName(), flight.getFlightNumber());
            }

            updateAvailableSeats(flight);
        }

        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setTransactionId(request.getPayment().getTransactionId());
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(request.getPayment().getPaymentMethod());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        savedBooking.setStatus(BookingStatus.CONFIRMED);
        savedBooking.setTotalAmount(totalAmount);

        Booking finalBooking = bookingRepository.save(savedBooking);
        log.info("✅ Booking created successfully! PNR: {}, Total: ₹{}",
                finalBooking.getPnrNumber(), totalAmount);

        for (Passenger p : passengers) {
            log.info("Passenger: {} - Email: {}, Phone: {}",
                    p.getFullName(), p.getEmail(), p.getPhoneNumber());
        }

        return finalBooking;
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random random = new Random();

        do {
            pnr.setLength(0);
            for (int i = 0; i < 6; i++) {
                pnr.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (bookingRepository.existsByPnrNumber(pnr.toString()));

        return pnr.toString();
    }

    private void updateAvailableSeats(Flight flight) {
        Long bookedCount = passengerSeatRepository.countBookedSeatsForFlight(flight.getId());
        int totalSeats = flight.getAircraft().getTotalSeats();
        flight.setAvailableEconomySeats(totalSeats - bookedCount.intValue());
        flightRepository.save(flight);
    }

    public Booking findByPNR(String pnr) throws BookingException {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new BookingException("PNR number cannot be empty");
        }
        return bookingRepository.findByPnrNumber(pnr.toUpperCase())
                .orElseThrow(() -> new BookingException("Booking not found with PNR: " + pnr));
    }

    public List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status) throws BookingException {
        if (userId == null) throw new BookingException("User ID cannot be null");
        if (status == null) throw new BookingException("Status cannot be null");
        return bookingRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) throws BookingException {
        log.info("Cancelling booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Completed bookings cannot be cancelled");
        }

        releaseSeats(booking);
        booking.setStatus(BookingStatus.CANCELLED);

        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("✅ Booking {} cancelled successfully", booking.getPnrNumber());
        return cancelledBooking;
    }

    public Booking findById(Long bookingId) throws BookingException {
        if (bookingId == null) throw new BookingException("Booking ID cannot be null");
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + bookingId));
    }

    @Transactional
    public Booking getBookingDetails(Long bookingId) throws BookingException {
        Booking booking = findById(bookingId);
        booking.getPassengers().size();
        booking.getBookingFlights().size();
        if (booking.getPayment() != null) {
            booking.getPayment().getTransactionId();
        }
        return booking;
    }

    private void releaseSeats(Booking booking) {
        List<BookingFlight> bookingFlights = booking.getBookingFlights();
        for (BookingFlight bookingFlight : bookingFlights) {
            List<PassengerSeat> passengerSeats = bookingFlight.getPassengerSeats();
            for (PassengerSeat passengerSeat : passengerSeats) {
                passengerSeatRepository.delete(passengerSeat);
                log.debug("Released seat ID: {}", passengerSeat.getSeat().getId());
            }
            Flight flight = bookingFlight.getFlight();
            updateAvailableSeats(flight);
        }
    }

    public List<Booking> findByUserId(Long userId) throws BookingException {
        if (userId == null) throw new BookingException("User ID cannot be null");
        if (!userRepository.existsById(userId)) {
            throw new BookingException("User not found with ID: " + userId);
        }
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getRecentUserBookings(Long userId, int limit) throws BookingException {
        if (userId == null) throw new BookingException("User ID cannot be null");
        List<Booking> allBookings = findByUserId(userId);
        return allBookings.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
}