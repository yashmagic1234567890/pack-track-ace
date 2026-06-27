package com.freshtrack.repository;

import com.freshtrack.entity.Invoice;
import com.freshtrack.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceBusinessId(String invoiceBusinessId);

    boolean existsByInvoiceBusinessId(String invoiceBusinessId);

    List<Invoice> findByWarehouse_WarehouseCode(String warehouseCode);

    List<Invoice> findByWarehouse_WarehouseCodeAndStatusNot(String warehouseCode, InvoiceStatus status);

    /**
     * Reconciliation filter: optional date range, warehouse and vendor.
     * Null parameters are ignored (treated as "match all").
     */
    @Query("""
            SELECT i FROM Invoice i
            WHERE (:warehouseCode IS NULL OR i.warehouse.warehouseCode = :warehouseCode)
              AND (:vendorName   IS NULL OR LOWER(i.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')))
              AND (:from IS NULL OR i.createdAt >= :from)
              AND (:to   IS NULL OR i.createdAt <= :to)
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> filterForReconciliation(@Param("warehouseCode") String warehouseCode,
                                          @Param("vendorName") String vendorName,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    long countByStatus(InvoiceStatus status);
}
