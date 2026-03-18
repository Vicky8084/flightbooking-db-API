package db_api.db_api.service;

import db_api.db_api.dto.ConnectingFlightDTO;
import db_api.db_api.dto.FlightSearchDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.model.Airport;
import db_api.db_api.model.Flight;
import db_api.db_api.repository.AirportRepository;
import db_api.db_api.repository.FlightRepository;
import db_api.db_api.repository.FlightSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightSearchService {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightSegmentRepository flightSegmentRepository;

    public List<Object> searchFlights(FlightSearchDTO searchDTO) {
        Airport source = airportRepository.findByCode(searchDTO.getSourceCode())
                .orElseThrow(() -> new RuntimeException("Source airport not found: " + searchDTO.getSourceCode()));

        Airport destination = airportRepository.findByCode(searchDTO.getDestinationCode())
                .orElseThrow(() -> new RuntimeException("Destination airport not found: " + searchDTO.getDestinationCode()));

        LocalDateTime startOfDay = searchDTO.getTravelDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Object> allResults = new ArrayList<>();

        // 1. Search direct flights
        List<Flight> directFlights = findDirectFlights(source, destination, startOfDay, endOfDay);
        allResults.addAll(directFlights);

        // 2. Search connecting flights if requested
        if (searchDTO.getIncludeConnectingFlights()) {
            List<ConnectingFlightDTO> connectingFlights = findConnectingFlights(source, destination, startOfDay, endOfDay);
            allResults.addAll(connectingFlights);
        }

        // 3. Apply price filter
        if (searchDTO.getMaxPrice() != null) {
            allResults = filterByPrice(allResults, searchDTO.getMaxPrice());
        }

        return allResults;
    }

    /**
     * Find direct flights
     */
    private List<Flight> findDirectFlights(Airport source, Airport destination,
                                           LocalDateTime startOfDay, LocalDateTime endOfDay) {
        List<Flight> directFlights = flightRepository.findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
                source.getCode(), destination.getCode(), FlightStatus.SCHEDULED);

        return directFlights.stream()
                .filter(f -> !f.getDepartureTime().isBefore(startOfDay) &&
                        !f.getDepartureTime().isAfter(endOfDay))
                .collect(Collectors.toList());
    }

    /**
     * Find connecting flights (2 segments)
     */
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

                    // ✅ CORRECT: Create connecting flight with both segments
                    List<Flight> segments = new ArrayList<>();
                    segments.add(firstLeg);
                    segments.add(secondLeg);

                    ConnectingFlightDTO connectingFlight = new ConnectingFlightDTO(segments);
                    connectingFlights.add(connectingFlight);
                }
            }

            // Also look for three-segment connections
            findThreeSegmentConnections(firstLeg, destination, connectingFlights);
        }

        return connectingFlights;
    }

    /**
     * Find connecting flights with 3 segments (optional)
     */
    private void findThreeSegmentConnections(Flight firstLeg, Airport destination,
                                             List<ConnectingFlightDTO> connectingFlights) {
        // Find second leg to another intermediate airport
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

    /**
     * Filter results by price
     */
    private List<Object> filterByPrice(List<Object> results, Double maxPrice) {
        return results.stream()
                .filter(obj -> {
                    if (obj instanceof Flight) {
                        return ((Flight) obj).getBasePriceEconomy() <= maxPrice;
                    } else if (obj instanceof ConnectingFlightDTO) {
                        return ((ConnectingFlightDTO) obj).getTotalPrice() <= maxPrice;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}