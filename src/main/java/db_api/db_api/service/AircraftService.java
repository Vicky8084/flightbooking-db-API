package db_api.db_api.service;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.SeatClass;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Airline;
import db_api.db_api.repository.AircraftRepository;
import db_api.db_api.repository.AirlineRepository;
import db_api.db_api.util.AircraftSeatValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftService {

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirlineRepository airlineRepository;  // ✅ Changed from UserRepository
    @Autowired

    private AircraftSeatValidator seatValidator;
    @Autowired
    private SeatGeneratorService seatGeneratorService;


    public Aircraft createAircraft(Aircraft aircraft) throws BookingException {
        // Check if registration number already exists
        if (aircraftRepository.existsByRegistrationNumber(aircraft.getRegistrationNumber())) {
            throw new BookingException("Registration number already exists: " + aircraft.getRegistrationNumber());
        }

        // Verify airline exists
        Airline airline = airlineRepository.findById(aircraft.getAirline().getId())
                .orElseThrow(() -> new BookingException("Airline not found with ID: " + aircraft.getAirline().getId()));

        if (airline.getStatus() != AccountStatus.ACTIVE) {
            throw new BookingException("Airline is not active. Status: " + airline.getStatus());
        }

        // Calculate total seats
        int totalSeats = aircraft.getEconomySeats() + aircraft.getBusinessSeats() + aircraft.getFirstClassSeats();
        aircraft.setTotalSeats(totalSeats);

        // Validate seat distribution
        seatValidator.validateSeatDistribution(totalSeats,
                aircraft.getEconomySeats(),
                aircraft.getBusinessSeats(),
                aircraft.getFirstClassSeats());

        aircraft.setAirline(airline);

        // Set default values if not provided
        if (aircraft.getWindowSeatPremiumPercent() == null) aircraft.setWindowSeatPremiumPercent(15);
        if (aircraft.getAisleSeatPremiumPercent() == null) aircraft.setAisleSeatPremiumPercent(10);
        if (aircraft.getMiddleSeatDiscountPercent() == null) aircraft.setMiddleSeatDiscountPercent(5);
        if (aircraft.getExtraLegroomPremium() == null) aircraft.setExtraLegroomPremium(1000.0);
        if (aircraft.getExitRowPremium() == null) aircraft.setExitRowPremium(750.0);

        Aircraft savedAircraft = aircraftRepository.save(aircraft);

        // Generate seats automatically
        seatGeneratorService.generateSeatsForAircraft(savedAircraft);

        return savedAircraft;
    }


    public Aircraft getAircraftById(Long id) throws BookingException {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new BookingException("Aircraft not found with ID: " + id));
    }

    public List<Aircraft> getAllAircrafts() {
        return aircraftRepository.findAll();
    }

    public List<Aircraft> getAircraftsByAirline(Long airlineId) {
        return aircraftRepository.findByAirlineId(airlineId);
    }

    public List<Aircraft> getAircraftsByModel(String model) {
        return aircraftRepository.findByModel(model);
    }

    public List<Aircraft> getAircraftsByManufacturer(String manufacturer) {
        return aircraftRepository.findByManufacturer(manufacturer);
    }

    public Aircraft updateAircraft(Long id, Aircraft aircraftDetails) throws BookingException {
        Aircraft aircraft = getAircraftById(id);

        aircraft.setModel(aircraftDetails.getModel());
        aircraft.setManufacturer(aircraftDetails.getManufacturer());
        aircraft.setTotalSeats(aircraftDetails.getTotalSeats());
        aircraft.setEconomySeats(aircraftDetails.getEconomySeats());
        aircraft.setBusinessSeats(aircraftDetails.getBusinessSeats());
        aircraft.setFirstClassSeats(aircraftDetails.getFirstClassSeats());

        return aircraftRepository.save(aircraft);
    }

    public void deleteAircraft(Long id) throws BookingException {
        Aircraft aircraft = getAircraftById(id);

        // ✅ Check if aircraft has any flights
        if (aircraft.getFlights() != null && !aircraft.getFlights().isEmpty()) {
            throw new BookingException("Cannot delete aircraft with existing flights");
        }

        aircraftRepository.delete(aircraft);
    }
}