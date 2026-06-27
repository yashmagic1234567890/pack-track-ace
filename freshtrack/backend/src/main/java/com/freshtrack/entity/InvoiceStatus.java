package com.freshtrack.entity;

/**
 * Lifecycle status of an invoice within the receiving workflow.
 */
public enum InvoiceStatus {
    /** Uploaded, no items received yet. */
    PENDING,
    /** Some but not all items received. */
    IN_PROGRESS,
    /** Received quantity meets or exceeds expected for every line. */
    COMPLETED
}
