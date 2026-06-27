package com.freshtrack.entity;

/**
 * Types of auditable actions in the receiving workflow.
 */
public enum AuditAction {
    LOGIN,
    INVOICE_UPLOAD,
    SCAN,
    MANUAL_INCREMENT,
    MANUAL_DECREMENT,
    OVERRIDE,
    USER_CREATED,
    WAREHOUSE_MAPPING_UPDATED,
    REPORT_EXPORT
}
