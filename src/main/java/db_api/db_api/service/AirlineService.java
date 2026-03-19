package db_api.db_api.service;

import db_api.db_api.enums.AccountStatus;
import db_api.db_api.enums.UserRole;
import db_api.db_api.exception.BookingException;
import db_api.db_api.model.Airline;
import db_api.db_api.model.User;
import db_api.db_api.repository.AirlineRepository;
import db_api.db_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AirlineService {

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private UserRepository userRepository;

    public Airline createAirline(Airline airline) throws BookingException {
        // Check if airline code already exists
        if (airlineRepository.existsByCode(airline.getCode())) {
            throw new BookingException("Airline code already exists: " + airline.getCode());
        }

        // Check if airline name already exists
        if (airlineRepository.existsByName(airline.getName())) {
            throw new BookingException("Airline name already exists: " + airline.getName());
        }

        return airlineRepository.save(airline);
    }

    public Airline getAirlineById(Long id) throws BookingException {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new BookingException("Airline not found with ID: " + id));
    }

    public Airline getAirlineByCode(String code) throws BookingException {
        return airlineRepository.findByCode(code)
                .orElseThrow(() -> new BookingException("Airline not found with code: " + code));
    }

    public List<Airline> getAllAirlines() {
        return airlineRepository.findAll();
    }

    public List<Airline> getPendingAirlines() {
        return airlineRepository.findPendingAirlines();
    }

    public List<Airline> getActiveAirlines() {
        return airlineRepository.findActiveAirlines();
    }

    @Transactional
    public Airline approveAirline(Long airlineId, Long adminId) throws BookingException {
        Airline airline = getAirlineById(airlineId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found with ID: " + adminId));

        // Check if admin is SYSTEM_ADMIN
        if (admin.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can approve airlines");
        }

        // Check current status
        if (airline.getStatus() == AccountStatus.ACTIVE) {
            throw new BookingException("Airline is already active");
        }

        // Update airline status
        airline.setStatus(AccountStatus.ACTIVE);
        airline.setApprovedBy(admin);
        airline.setApprovedAt(LocalDateTime.now());

        // Update all admins of this airline to ACTIVE
        List<User> airlineAdmins = userRepository.findByAirlineId(airlineId);
        for (User adminUser : airlineAdmins) {
            adminUser.setStatus(AccountStatus.ACTIVE);
            adminUser.setApprovedBy(admin);
            adminUser.setApprovedAt(LocalDateTime.now());
            userRepository.save(adminUser);
        }

        return airlineRepository.save(airline);
    }

    @Transactional
    public Airline rejectAirline(Long airlineId, Long adminId, String reason) throws BookingException {
        Airline airline = getAirlineById(airlineId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found with ID: " + adminId));

        if (admin.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can reject airlines");
        }

        airline.setStatus(AccountStatus.REJECTED);
        airline.setRejectedBy(admin);
        airline.setRejectedAt(LocalDateTime.now());
        airline.setRejectionReason(reason);

        // Update all admins of this airline to REJECTED
        List<User> airlineAdmins = userRepository.findByAirlineId(airlineId);
        for (User adminUser : airlineAdmins) {
            adminUser.setStatus(AccountStatus.REJECTED);
            adminUser.setApprovedBy(admin);
            adminUser.setApprovedAt(LocalDateTime.now());
            userRepository.save(adminUser);
        }

        return airlineRepository.save(airline);
    }

    @Transactional
    public Airline suspendAirline(Long airlineId, Long adminId) throws BookingException {
        Airline airline = getAirlineById(airlineId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BookingException("Admin not found with ID: " + adminId));

        if (admin.getRole() != UserRole.SYSTEM_ADMIN) {
            throw new BookingException("Only SYSTEM_ADMIN can suspend airlines");
        }

        airline.setStatus(AccountStatus.SUSPENDED);

        // Suspend all admins of this airline
        List<User> airlineAdmins = userRepository.findByAirlineId(airlineId);
        for (User adminUser : airlineAdmins) {
            adminUser.setStatus(AccountStatus.SUSPENDED);
            userRepository.save(adminUser);
        }

        return airlineRepository.save(airline);
    }

    public Airline updateAirline(Long id, Airline airlineDetails) throws BookingException {
        Airline airline = getAirlineById(id);

        airline.setName(airlineDetails.getName());
        airline.setContactEmail(airlineDetails.getContactEmail());
        airline.setContactPhone(airlineDetails.getContactPhone());
        airline.setAddress(airlineDetails.getAddress());
        airline.setWebsite(airlineDetails.getWebsite());

        return airlineRepository.save(airline);
    }

    public void deleteAirline(Long id) throws BookingException {
        Airline airline = getAirlineById(id);

        // Check if airline has any admins
        List<User> admins = userRepository.findByAirlineId(id);
        if (!admins.isEmpty()) {
            throw new BookingException("Cannot delete airline with existing admins");
        }

        airlineRepository.delete(airline);
    }
}