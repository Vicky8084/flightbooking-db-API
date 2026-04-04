package db_api.db_api.service;

import db_api.db_api.dto.ConnectingFlightDTO;
import db_api.db_api.dto.FlightSearchDTO;
import db_api.db_api.enums.FlightStatus;
import db_api.db_api.model.Airport;
import db_api.db_api.model.Flight;
import db_api.db_api.repository.AirportRepository;
import db_api.db_api.repository.FlightRepository;
import db_api.db_api.repository.FlightSegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlightSearchService {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightSegmentRepository flightSegmentRepository;

    // ✅ NEW: Hub airports from properties
    @Value("${app.hub.airports:BOM,DEL,BLR}")
    private String hubAirportsConfig;

    @Value("${app.primary.hub:BOM}")
    private String primaryHub;

    public List<Object> searchFlights(FlightSearchDTO searchDTO) {
        Airport source = airportRepository.findByCode(searchDTO.getSourceCode())
                .orElseThrow(() -> new RuntimeException("Source airport not found: " + searchDTO.getSourceCode()));

        Airport destination = airportRepository.findByCode(searchDTO.getDestinationCode())
                .orElseThrow(() -> new RuntimeException("Destination airport not found: " + searchDTO.getDestinationCode()));

        LocalDateTime startOfDay = searchDTO.getTravelDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        // ✅ ADD THIS - If travel date is today, only show flights after current time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveStart = startOfDay;

        if (searchDTO.getTravelDate().isEqual(now.toLocalDate())) {
            // Today's date - only show flights after current time
            effectiveStart = now;
            log.info("Today's search - filtering flights after current time: {}", now);
        }

        List<Object> allResults = new ArrayList<>();

        // 1. Search direct flights with time validation
        List<Flight> directFlights = findDirectFlights(source, destination, effectiveStart, endOfDay);
        allResults.addAll(directFlights);

        // 2. Search connecting flights with time validation
        if (searchDTO.getIncludeConnectingFlights()) {
            List<ConnectingFlightDTO> hubConnections = findHubBasedConnections(source, destination, effectiveStart, endOfDay);
            allResults.addAll(hubConnections);

            if (hubConnections.isEmpty()) {
                List<ConnectingFlightDTO> normalConnections = findNormalConnectingFlights(source, destination, effectiveStart, endOfDay);
                allResults.addAll(normalConnections);
            }
        }

        // 3. Apply price filter
        if (searchDTO.getMaxPrice() != null) {
            allResults = filterByPrice(allResults, searchDTO.getMaxPrice());
        }

        return allResults;
    }

    /**
     * ✅ NEW: Find hub-based connections (preferred)
     */
    private List<ConnectingFlightDTO> findHubBasedConnections(Airport source, Airport destination,
                                                              LocalDateTime startTime, LocalDateTime endTime) {
        List<ConnectingFlightDTO> hubConnections = new ArrayList<>();
        List<String> hubCodes = Arrays.asList(hubAirportsConfig.split(","));

        for (String hubCode : hubCodes) {
            // Find flights from source to hub with time validation
            List<Flight> firstLegFlights = flightRepository
                    .findBySourceAirportCodeAndDepartureTimeBetween(source.getCode(), startTime, endTime);

            for (Flight firstLeg : firstLegFlights) {
                if (firstLeg.getDestinationAirport().getCode().equals(hubCode)) {
                    LocalDateTime minConnectionTime = firstLeg.getArrivalTime().plusHours(1);
                    LocalDateTime maxConnectionTime = firstLeg.getArrivalTime().plusHours(6);

                    List<Flight> secondLegFlights = flightRepository
                            .findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
                                    hubCode, destination.getCode(), FlightStatus.SCHEDULED);

                    for (Flight secondLeg : secondLegFlights) {
                        // ✅ Check if second leg departure is after min connection time
                        if (!secondLeg.getDepartureTime().isBefore(minConnectionTime) &&
                                !secondLeg.getDepartureTime().isAfter(maxConnectionTime)) {

                            List<Flight> segments = new ArrayList<>();
                            segments.add(firstLeg);
                            segments.add(secondLeg);

                            ConnectingFlightDTO connection = new ConnectingFlightDTO(segments);
                            connection.setConnectionType("HUB");
                            connection.setHubAirport(hubCode);
                            connection.setSameAirline(firstLeg.getAircraft().getAirline().getId()
                                    .equals(secondLeg.getAircraft().getAirline().getId()));

                            hubConnections.add(connection);
                        }
                    }
                }
            }
        }
        return hubConnections;
    }

    /**
     * Find direct flights
     */
    private List<Flight> findDirectFlights(Airport source, Airport destination,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        List<Flight> directFlights = flightRepository.findBySourceAirportCodeAndDestinationAirportCodeAndStatus(
                source.getCode(), destination.getCode(), FlightStatus.SCHEDULED);

        return directFlights.stream()
                .filter(f -> !f.getDepartureTime().isBefore(startTime) &&
                        !f.getDepartureTime().isAfter(endTime))
                .collect(Collectors.toList());
    }

    /**
     * Find normal connecting flights (2 segments) - fallback
     */
    private List<ConnectingFlightDTO> findNormalConnectingFlights(Airport source, Airport destination,
                                                                  LocalDateTime startOfDay, LocalDateTime endOfDay) {
        List<ConnectingFlightDTO> connectingFlights = new ArrayList<>();

        List<Flight> firstLegFlights = flightRepository
                .findBySourceAirportCodeAndDepartureTimeBetween(source.getCode(), startOfDay, endOfDay);

        for (Flight firstLeg : firstLegFlights) {
            // Skip if first leg goes to destination
            if (firstLeg.getDestinationAirport().getCode().equals(destination.getCode())) {
                continue;
            }

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

                    List<Flight> segments = new ArrayList<>();
                    segments.add(firstLeg);
                    segments.add(secondLeg);

                    ConnectingFlightDTO connection = new ConnectingFlightDTO(segments);
                    connection.setConnectionType("NORMAL");
                    connection.setSameAirline(firstLeg.getAircraft().getAirline().getId()
                            .equals(secondLeg.getAircraft().getAirline().getId()));

                    connectingFlights.add(connection);
                }
            }

            // Find three-segment connections
            findThreeSegmentConnections(firstLeg, destination, connectingFlights);
        }

        return connectingFlights;
    }

    /**
     * Find connecting flights with 3 segments
     */
    private void findThreeSegmentConnections(Flight firstLeg, Airport destination,
                                             List<ConnectingFlightDTO> connectingFlights) {
        List<Flight> secondLegFlights = flightRepository
                .findBySourceAirportCodeAndDepartureTimeBetween(
                        firstLeg.getDestinationAirport().getCode(),
                        firstLeg.getArrivalTime().plusHours(1),
                        firstLeg.getArrivalTime().plusHours(4));

        for (Flight secondLeg : secondLegFlights) {
            if (secondLeg.getDestinationAirport().getCode().equals(destination.getCode())) {
                continue;
            }

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

                    ConnectingFlightDTO connection = new ConnectingFlightDTO(segments);
                    connection.setConnectionType("NORMAL");
                    connection.setSameAirline(firstLeg.getAircraft().getAirline().getId()
                            .equals(secondLeg.getAircraft().getAirline().getId()) &&
                            secondLeg.getAircraft().getAirline().getId()
                                    .equals(thirdLeg.getAircraft().getAirline().getId()));

                    connectingFlights.add(connection);
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