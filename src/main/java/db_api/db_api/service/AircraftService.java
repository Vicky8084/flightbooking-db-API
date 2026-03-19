package db_api.db_api.service;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Aircraft;
import db_api.db_api.model.Airline;
import db_api.db_api.repository.AircraftRepository;
import db_api.db_api.repository.AirlineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftService {

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirlineRepository airlineRepository;  // ✅ Changed from UserRepository

    public Aircraft createAircraft(Aircraft aircraft) throws BookingException {
        // Check if registration number already exists
        if (aircraftRepository.existsByRegistrationNumber(aircraft.getRegistrationNumber())) {
            throw new BookingException("Registration number already exists: " + aircraft.getRegistrationNumber());
        }

        // ✅ Verify airline exists in Airline table (not User table)
        Airline airline = airlineRepository.findById(aircraft.getAirline().getId())
                .orElseThrow(() -> new BookingException("Airline not found with ID: " + aircraft.getAirline().getId()));

        // ✅ Check if airline is ACTIVE
        if (airline.getStatus() != AccountStatus.ACTIVE) {
            throw new BookingException("Airline is not active. Status: " + airline.getStatus());
        }

        aircraft.setAirline(airline);

        return aircraftRepository.save(aircraft);
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