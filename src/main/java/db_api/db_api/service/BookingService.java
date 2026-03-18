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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
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

            // Check flight availability
            if (flight.getStatus() != FlightStatus.SCHEDULED) {
                throw new BookingException("Flight is not available for booking: " + flight.getFlightNumber());
            }

            // ✅ Create AND SAVE booking flight
            BookingFlight bookingFlight = new BookingFlight();
            bookingFlight.setBooking(savedBooking);
            bookingFlight.setFlight(flight);
            bookingFlight.setFlightSequence(flightDTO.getSequence() != null ? flightDTO.getSequence() : 1);

            // ✅ IMPORTANT: Save bookingFlight before using it in PassengerSeat
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

                // Check seat availability
                Seat seat = seatRepository.findById(psDTO.getSeatId())
                        .orElseThrow(() -> new BookingException("Seat not found"));

                // Verify seat belongs to this aircraft
                if (!seat.getAircraft().getId().equals(flight.getAircraft().getId())) {
                    throw new BookingException("Seat does not belong to this aircraft");
                }

                // Check if seat is already booked
                if (passengerSeatRepository.isSeatBookedForFlight(seat.getId(), flight.getId())) {
                    throw new BookingException("Seat " + seat.getSeatNumber() + " is already booked");
                }

                // Calculate seat price
                double seatPrice = calculateSeatPrice(flight, seat);
                totalAmount += seatPrice;

                // ✅ Use savedBookingFlight instead of unsaved bookingFlight
                PassengerSeat passengerSeat = new PassengerSeat();
                passengerSeat.setBookingFlight(savedBookingFlight);  // ✅ FIXED
                passengerSeat.setPassenger(passenger);
                passengerSeat.setSeat(seat);
                passengerSeat.setSeatPrice(seatPrice);

                passengerSeatRepository.save(passengerSeat);
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

        return bookingRepository.save(savedBooking);
    }
    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random random = new Random();

        do {
            for (int i = 0; i < 6; i++) {
                pnr.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (bookingRepository.existsByPnrNumber(pnr.toString()));

        return pnr.toString();
    }

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

        // Add extra for special seats
        if (seat.getSeatType() == SeatType.WINDOW) {
            basePrice += 500; // Window seat premium
        }

        if (seat.getHasExtraLegroom()) {
            basePrice += 1000; // Extra legroom premium
        }

        if (seat.getIsNearExit()) {
            basePrice += 750; // Exit row premium
        }

        // Add any configured extra price
        basePrice += seat.getExtraPrice() != null ? seat.getExtraPrice() : 0;

        return basePrice;
    }

    private void updateAvailableSeats(Flight flight) {
        // Count booked seats for this flight
        Long bookedCount = passengerSeatRepository.countBookedSeatsForFlight(flight.getId());

        // Update available counts based on seat class
        // This is simplified - in reality, you'd track per class
        int totalSeats = flight.getAircraft().getTotalSeats();
        flight.setAvailableEconomySeats(totalSeats - bookedCount.intValue());

        flightRepository.save(flight);
    }

    // service/BookingService.java (mein ye methods add karo)

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
     * Find all bookings by user ID
     */
    public List<Booking> findByUserId(Long userId) throws BookingException {
        if (userId == null) {
            throw new BookingException("User ID cannot be null");
        }

        // Check if user exists
        if (!userRepository.existsById(userId)) {
            throw new BookingException("User not found with ID: " + userId);
        }

        List<Booking> bookings = bookingRepository.findByUserId(userId);

        if (bookings.isEmpty()) {
            throw new BookingException("No bookings found for user ID: " + userId);
        }

        return bookings;
    }

    /**
     * Find bookings by user ID and status (optional utility method)
     */
    public List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status) throws BookingException {
        if (userId == null || status == null) {
            throw new BookingException("User ID and Status cannot be null");
        }

        return bookingRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) throws BookingException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with ID: " + bookingId));

        // Check if booking can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Completed bookings cannot be cancelled");
        }

        // RELEASE SEATS - Make them available again
        releaseSeats(booking);

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);

        // Update payment status to REFUNDED
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }

        return bookingRepository.save(booking);
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
     * Get booking details with all associations (eager loading)
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
        // Get all passenger seats for this booking
        List<BookingFlight> bookingFlights = booking.getBookingFlights();

        for (BookingFlight bookingFlight : bookingFlights) {
            List<PassengerSeat> passengerSeats = bookingFlight.getPassengerSeats();

            // Delete all passenger seat assignments
            for (PassengerSeat passengerSeat : passengerSeats) {
                passengerSeatRepository.delete(passengerSeat);
            }

            // Update available seats count for the flight
            Flight flight = bookingFlight.getFlight();
            updateAvailableSeats(flight);
        }
    }


}
