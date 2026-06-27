package com.freshtrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Physical fulfillment hub / warehouse. The business identifier is
 * {@code warehouseCode} (the {@code Target_Warehouse_ID} referenced in invoices).
 */
@Entity
@Table(name = "warehouses",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_code", columnNames = "warehouse_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business identifier, e.g. "WH-DEL-01". Maps to Target_Warehouse_ID. */
    @Column(name = "warehouse_code", nullable = false, length = 50)
    private String warehouseCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 250)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
