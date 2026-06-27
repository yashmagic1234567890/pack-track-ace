package com.freshtrack.repository;

import com.freshtrack.entity.InvoiceLine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findByInvoice_Id(Long invoiceId);

    Optional<InvoiceLine> findByInvoice_IdAndItemSku(Long invoiceId, String itemSku);

    /**
     * Pessimistic write lock used during high-frequency scans to guarantee
     * no count is skipped under concurrent rapid-fire input.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM InvoiceLine l WHERE l.invoice.id = :invoiceId AND l.itemSku = :sku")
    Optional<InvoiceLine> findForUpdate(@Param("invoiceId") Long invoiceId, @Param("sku") String sku);
}
