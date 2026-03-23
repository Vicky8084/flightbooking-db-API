package db_api.db_api.service;

import db_api.db_api.dto.BookingRequestDTO;
import db_api.db_api.dto.FlightBookingDTO;
import db_api.db_api.dto.PassengerDTO;
import db_api.db_api.dto.PassengerSeatDTO;
import db_api.db_api.enums.BookingStatus;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.enums.PaymentStatus;
import db_api.db_api.enums.SeatType;
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

    @Transactional
    public Booking createBooking(BookingRequestDTO request) throws BookingException {
        log.info("Creating booking for user ID: {}", request.getUserId());

        // Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BookingException("User not found"));

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPnrNumber(generatePNR());
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        // Save booking first
        Booking savedBooking = bookingRepository.save(booking);

        List<Passenger> passengers = new ArrayList<>();
        double totalAmount = 0.0;

        // Process each flight in the booking
        for (FlightBookingDTO flightDTO : request.getFlights()) {
            Flight flight = flightRepository.findById(flightDTO.getFlightId())
                    .orElseThrow(() -> new BookingException("Flight not found: " + flightDTO.getFlightId()));

            // ✅ Validate booking is still allowed (with dynamic pricing check)
            if (!flight.canBook()) {
                throw new BookingException("Booking for flight " + flight.getFlightNumber() +
                        " is closed. Cutoff time was: " +
                        flight.getDepartureTime().minusHours(flight.getBookingCutoffHours()));
            }

            // Check flight availability
            if (flight.getStatus() != FlightStatus.SCHEDULED) {
                throw new BookingException("Flight is not available for booking: " + flight.getFlightNumber());
            }

            // Create AND save booking flight
            BookingFlight bookingFlight = new BookingFlight();
            bookingFlight.setBooking(savedBooking);
            bookingFlight.setFlight(flight);
            bookingFlight.setFlightSequence(flightDTO.getSequence() != null ? flightDTO.getSequence() : 1);

            // Save bookingFlight before using it in PassengerSeat
            BookingFlight savedBookingFlight = bookingFlightRepository.save(bookingFlight);

            // For each passenger in this flight
            for (PassengerSeatDTO psDTO : flightDTO.getPassengerSeats()) {
                int passengerIndex = psDTO.getPassengerIndex();

                // Get or create passenger
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
                    passenger = passengerRepository.save(passenger);
                    passengers.add(passenger);
                }

                // Check seat availability with pessimistic lock
                Seat seat = seatRepository.findById(psDTO.getSeatId())
                        .orElseThrow(() -> new BookingException("Seat not found"));

                // Verify seat belongs to this aircraft
                if (!seat.getAircraft().getId().equals(flight.getAircraft().getId())) {
                    throw new BookingException("Seat does not belong to this aircraft");
                }

                // ✅ Check if seat is already booked (with lock)
                if (passengerSeatRepository.isSeatBookedForFlight(seat.getId(), flight.getId())) {
                    throw new BookingException("Seat " + seat.getSeatNumber() + " is already booked");
                }

                // ✅ Calculate seat price using dynamic pricing
                double flightBasePrice = flight.getCurrentPrice(seat.getSeatClass().name());
                double seatPrice = seat.calculateFinalPrice(flightBasePrice);
                totalAmount += seatPrice;

                // Create passenger seat assignment
                PassengerSeat passengerSeat = new PassengerSeat();
                passengerSeat.setBookingFlight(savedBookingFlight);
                passengerSeat.setPassenger(passenger);
                passengerSeat.setSeat(seat);
                passengerSeat.setSeatPrice(seatPrice);

                passengerSeatRepository.save(passengerSeat);

                log.debug("Seat {} assigned to passenger {} for flight {}",
                        seat.getSeatNumber(), passenger.getFullName(), flight.getFlightNumber());
            }

            // Update available seats count on flight
            updateAvailableSeats(flight);
        }

        // Process payment
        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setTransactionId(request.getPayment().getTransactionId());
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(request.getPayment().getPaymentMethod());
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Update booking status
        savedBooking.setStatus(BookingStatus.CONFIRMED);
        savedBooking.setTotalAmount(totalAmount);

        Booking finalBooking = bookingRepository.save(savedBooking);
        log.info("✅ Booking created successfully! PNR: {}, Total: ₹{}",
                finalBooking.getPnrNumber(), totalAmount);

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

    /**
     * Find booking by PNR number
     */
    public Booking findByPNR(String pnr) throws BookingException {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new BookingException("PNR number cannot be empty");
        }

        return bookingRepository.findByPnrNumber(pnr.toUpperCase())
                .orElseThrow(() -> new BookingException("Booking not found with PNR: " + pnr));
    }

    /**
     * Find bookings by user ID and status
     */
    public List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status) throws BookingException {
        if (userId == null) {
            throw new BookingException("User ID cannot be null");
        }
        if (status == null) {
            throw new BookingException("Status cannot be null");
        }

        return bookingRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Cancel booking and release seats
     */
    @Transactional
    public Booking cancelBooking(Long bookingId) throws BookingException {
        log.info("Cancelling booking ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + bookingId));

        // Check if booking can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Completed bookings cannot be cancelled");
        }

        // Release seats
        releaseSeats(booking);

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);

        // Update payment status to REFUNDED
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }

        Booking cancelledBooking = bookingRepository.save(booking);
        log.info("✅ Booking {} cancelled successfully", booking.getPnrNumber());

        return cancelledBooking;
    }

    /**
     * Find booking by ID
     */
    public Booking findById(Long bookingId) throws BookingException {
        if (bookingId == null) {
            throw new BookingException("Booking ID cannot be null");
        }

        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + bookingId));
    }

    /**
     * Get booking details with all associations
     */
    @Transactional
    public Booking getBookingDetails(Long bookingId) throws BookingException {
        Booking booking = findById(bookingId);

        // Force load lazy associations
        booking.getPassengers().size();
        booking.getBookingFlights().size();
        if (booking.getPayment() != null) {
            booking.getPayment().getTransactionId();
        }

        return booking;
    }

    /**
     * Release all seats for this booking
     */
    private void releaseSeats(Booking booking) {
        List<BookingFlight> bookingFlights = booking.getBookingFlights();

        for (BookingFlight bookingFlight : bookingFlights) {
            List<PassengerSeat> passengerSeats = bookingFlight.getPassengerSeats();

            // Delete all passenger seat assignments
            for (PassengerSeat passengerSeat : passengerSeats) {
                passengerSeatRepository.delete(passengerSeat);
                log.debug("Released seat ID: {}", passengerSeat.getSeat().getId());
            }

            // Update available seats count for the flight
            Flight flight = bookingFlight.getFlight();
            updateAvailableSeats(flight);
        }
    }

    /**
     * Find all bookings by user ID
     */
    public List<Booking> findByUserId(Long userId) throws BookingException {
        if (userId == null) {
            throw new BookingException("User ID cannot be null");
        }

        if (!userRepository.existsById(userId)) {
            throw new BookingException("User not found with ID: " + userId);
        }

        return bookingRepository.findByUserId(userId);
    }

    /**
     * Get recent bookings for user
     */
    public List<Booking> getRecentUserBookings(Long userId, int limit) throws BookingException {
        if (userId == null) {
            throw new BookingException("User ID cannot be null");
        }

        List<Booking> allBookings = findByUserId(userId);
        return allBookings.stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }
}