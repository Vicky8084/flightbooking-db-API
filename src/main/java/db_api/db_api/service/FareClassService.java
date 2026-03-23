package db_api.db_api.service;

import db_api.db_api.exception.BookingException;
import db_api.db_api.model.FareClass;
import db_api.db_api.repository.FareClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FareClassService {

    private final FareClassRepository fareClassRepository;

    /**
     * Get all active fare classes
     */
    public List<FareClass> getAllActiveFareClasses() {
        // ✅ FIXED: Use correct method name
        return fareClassRepository.findByIsActiveTrue();
    }

    /**
     * Get fare class by code
     */
    public FareClass getFareClassByCode(String code) throws BookingException {
        return fareClassRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BookingException("Fare class not found: " + code));
    }

    /**
     * Create new fare class
     */
    public FareClass createFareClass(FareClass fareClass) throws BookingException {
        if (fareClassRepository.findByCode(fareClass.getCode()).isPresent()) {
            throw new BookingException("Fare class already exists: " + fareClass.getCode());
        }
        return fareClassRepository.save(fareClass);
    }

    /**
     * Update fare class
     */
    public FareClass updateFareClass(Long id, FareClass fareClassDetails) throws BookingException {
        FareClass fareClass = fareClassRepository.findById(id)
                .orElseThrow(() -> new BookingException("Fare class not found with ID: " + id));

        fareClass.setName(fareClassDetails.getName());
        fareClass.setDescription(fareClassDetails.getDescription());
        fareClass.setPriceMultiplier(fareClassDetails.getPriceMultiplier());
        fareClass.setCabinBaggageKg(fareClassDetails.getCabinBaggageKg());
        fareClass.setCheckInBaggageKg(fareClassDetails.getCheckInBaggageKg());
        fareClass.setExtraBaggageRatePerKg(fareClassDetails.getExtraBaggageRatePerKg());
        fareClass.setMealIncluded(fareClassDetails.getMealIncluded());
        fareClass.setCancellationFee(fareClassDetails.getCancellationFee());
        fareClass.setChangeFee(fareClassDetails.getChangeFee());
        fareClass.setRefundPercentageByDays(fareClassDetails.getRefundPercentageByDays());
        fareClass.setSeatSelectionFree(fareClassDetails.getSeatSelectionFree());
        fareClass.setPriorityCheckin(fareClassDetails.getPriorityCheckin());
        fareClass.setPriorityBoarding(fareClassDetails.getPriorityBoarding());
        fareClass.setLoungeAccess(fareClassDetails.getLoungeAccess());

        return fareClassRepository.save(fareClass);
    }

    /**
     * Calculate baggage cost for extra kg
     */
    public double calculateExtraBaggageCost(FareClass fareClass, int extraKg) {
        if (extraKg <= 0) return 0;
        return extraKg * fareClass.getExtraBaggageRatePerKg();
    }

    /**
     * Calculate cancellation refund based on days before departure
     */
    public double calculateRefundAmount(FareClass fareClass, double totalAmount, int daysBeforeDeparture) {
        if (fareClass.getRefundPercentageByDays() == null) {
            return Math.max(0, totalAmount - fareClass.getCancellationFee());
        }

        String[] rules = fareClass.getRefundPercentageByDays().split(",");
        for (String rule : rules) {
            String[] parts = rule.split(":");
            int thresholdDays = Integer.parseInt(parts[0]);
            double refundPercent = Double.parseDouble(parts[1]);
            if (daysBeforeDeparture >= thresholdDays) {
                return totalAmount * refundPercent / 100;
            }
        }

        return 0;
    }
}