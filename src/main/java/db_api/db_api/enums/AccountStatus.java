package db_api.db_api.enums;

public enum AccountStatus {
    PENDING,      // New airline/admin - waiting for approval
    ACTIVE,       // Approved and active
    SUSPENDED,    // Temporarily blocked
    REJECTED   // Registration rejected by admin
}