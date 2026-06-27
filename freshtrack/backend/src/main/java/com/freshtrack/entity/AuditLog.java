package com.freshtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Immutable audit record. Per the BRD, every barcode scan, manual increment or
 * override logs the timestamp, Invoice_ID, Item_SKU and the User_ID who acted.
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_invoice", columnList = "invoice_business_id"),
                @Index(name = "idx_audit_user", columnList = "username"),
                @Index(name = "idx_audit_ts", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AuditAction actionType;

    @Column(name = "invoice_business_id", length = 80)
    private String invoiceBusinessId;

    @Column(name = "item_sku", length = 100)
    private String itemSku;

    @Column(name = "warehouse_code", length = 50)
    private String warehouseCode;

    @Column(nullable = false, length = 100)
    private String username;

    /** Quantity change applied by this action (e.g. +1 for a scan). */
    @Column(name = "quantity_delta")
    private Integer quantityDelta;

    @Column(name = "resulting_quantity")
    private Integer resultingQuantity;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
