package db_api.db_api.service;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Airport;
import db_api.db_api.repository.AirportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AirportService {

    @Autowired
    private AirportRepository airportRepository;

    public Airport createAirport(Airport airport) throws BookingException {
        // Check if airport code already exists
        if (airportRepository.existsByCode(airport.getCode())) {
            throw new BookingException("Airport code already exists: " + airport.getCode());
        }

        return airportRepository.save(airport);
    }

    public Airport getAirportById(Long id) throws BookingException {
        return airportRepository.findById(id)
                .orElseThrow(() -> new BookingException("Airport not found with ID: " + id));
    }

    public Airport getAirportByCode(String code) throws BookingException {
        return airportRepository.findByCode(code)
                .orElseThrow(() -> new BookingException("Airport not found with code: " + code));
    }

    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    public List<Airport> getAirportsByCity(String city) {
        return airportRepository.findByCityContaining(city);
    }

    public List<Airport> getAirportsByCountry(String country) {
        return airportRepository.findByCountry(country);
    }

    public Airport updateAirport(Long id, Airport airportDetails) throws BookingException {
        Airport airport = getAirportById(id);

        airport.setName(airportDetails.getName());
        airport.setCity(airportDetails.getCity());
        airport.setCountry(airportDetails.getCountry());
        airport.setTimezone(airportDetails.getTimezone());

        return airportRepository.save(airport);
    }

    public void deleteAirport(Long id) throws BookingException {
        Airport airport = getAirportById(id);
        airportRepository.delete(airport);
    }
}
