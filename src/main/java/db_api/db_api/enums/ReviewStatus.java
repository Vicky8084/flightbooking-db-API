package db_api.db_api.enums;

public enum ReviewStatus {
    PENDING,    // Waiting for admin approval
    APPROVED,   // Published publicly
    REJECTED    // Rejected by admin
}