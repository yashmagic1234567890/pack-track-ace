package com.freshtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single line of an invoice = one SKU expected at a warehouse. Holds both the
 * expected quantity (from upload) and the running received quantity (updated by
 * scans / manual increments). This is the "ledger" record per item.
 */
@Entity
@Table(name = "invoice_lines",
        indexes = {
                @Index(name = "idx_line_invoice", columnList = "invoice_id"),
                @Index(name = "idx_line_sku", columnList = "item_sku")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_invoice_sku", columnNames = {"invoice_id", "item_sku"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "item_sku", nullable = false, length = 100)
    private String itemSku;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "expected_quantity", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "received_quantity", nullable = false)
    @Builder.Default
    private Integer receivedQuantity = 0;

    /** Optimistic locking to keep rapid-fire scans consistent. */
    @Version
    private Long version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Variance = Expected - Received (BRD reconciliation formula). */
    @Transient
    public int getVariance() {
        return expectedQuantity - receivedQuantity;
    }
}
