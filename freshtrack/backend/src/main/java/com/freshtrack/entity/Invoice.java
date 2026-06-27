package com.freshtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice header. A master invoice is uploaded by a Central Admin and targets a
 * single {@link Warehouse}. It groups one or more {@link InvoiceLine}s (the rows
 * of the uploaded CSV/Excel file).
 */
@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice_business_id", columnNames = "invoice_business_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The unique business identifier (Invoice_ID from the BRD). */
    @Column(name = "invoice_business_id", nullable = false, length = 80)
    private String invoiceBusinessId;

    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        this.lines.add(line);
    }
}
