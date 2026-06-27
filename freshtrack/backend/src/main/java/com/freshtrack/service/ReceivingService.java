package com.freshtrack.service;

import com.freshtrack.dto.ManualAdjustRequest;
import com.freshtrack.dto.ScanRequest;
import com.freshtrack.dto.ScanResultDto;
import com.freshtrack.entity.*;
import com.freshtrack.exception.BadRequestException;
import com.freshtrack.exception.ResourceNotFoundException;
import com.freshtrack.repository.InvoiceLineRepository;
import com.freshtrack.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core Scan-to-Receive engine. Each scan increments the matching invoice line's
 * received quantity by exactly +1 (BRD), updates the invoice status and writes an
 * audit record. A pessimistic write lock guarantees correctness under rapid scans.
 */
@Service
@RequiredArgsConstructor
public class ReceivingService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceService invoiceService;
    private final AuditService auditService;

    @Transactional
    public ScanResultDto scan(ScanRequest req, User user) {
        Invoice invoice = loadAndAuthorize(req.invoiceId(), req.warehouseCode(), user);

        InvoiceLine line = invoiceLineRepository
                .findForUpdate(invoice.getId(), req.itemSku())
                .orElseThrow(() -> new BadRequestException(
                        "SKU " + req.itemSku() + " is not part of invoice "
                                + invoice.getInvoiceBusinessId()));

        line.setReceivedQuantity(line.getReceivedQuantity() + 1);
        invoiceLineRepository.save(line);

        refreshStatus(invoice);

        auditService.log(AuditAction.SCAN, user.getUsername(), invoice.getInvoiceBusinessId(),
                line.getItemSku(), req.warehouseCode(), 1, line.getReceivedQuantity(),
                "Barcode scan +1");

        return buildResult(invoice, line);
    }

    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 4, backoff = @Backoff(delay = 50))
    public ScanResultDto adjust(ManualAdjustRequest req, User user) {
        Invoice invoice = loadAndAuthorize(req.invoiceId(), req.warehouseCode(), user);

        InvoiceLine line = invoiceLineRepository
                .findForUpdate(invoice.getId(), req.itemSku())
                .orElseThrow(() -> new BadRequestException(
                        "SKU " + req.itemSku() + " is not part of invoice "
                                + invoice.getInvoiceBusinessId()));

        AuditAction action;
        int resulting;
        Integer delta = req.delta();

        if (req.overrideValue() != null) {
            if (req.overrideValue() < 0) {
                throw new BadRequestException("Override value cannot be negative.");
            }
            int previous = line.getReceivedQuantity();
            resulting = req.overrideValue();
            delta = resulting - previous;
            action = AuditAction.OVERRIDE;
        } else if (delta != null && delta != 0) {
            resulting = line.getReceivedQuantity() + delta;
            if (resulting < 0) {
                throw new BadRequestException("Resulting quantity cannot be negative.");
            }
            action = delta > 0 ? AuditAction.MANUAL_INCREMENT : AuditAction.MANUAL_DECREMENT;
        } else {
            throw new BadRequestException("Provide a non-zero delta or an overrideValue.");
        }

        line.setReceivedQuantity(resulting);
        invoiceLineRepository.save(line);
        refreshStatus(invoice);

        String reason = req.reason() == null || req.reason().isBlank()
                ? "Manual adjustment" : req.reason();
        auditService.log(action, user.getUsername(), invoice.getInvoiceBusinessId(),
                line.getItemSku(), req.warehouseCode(), delta, resulting, reason);

        return buildResult(invoice, line);
    }

    // ----- helpers -----

    private Invoice loadAndAuthorize(Long invoiceId, String warehouseCode, User user) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getWarehouse().getWarehouseCode().equals(warehouseCode)) {
            throw new BadRequestException(
                    "Invoice " + invoice.getInvoiceBusinessId()
                            + " does not belong to warehouse " + warehouseCode);
        }
        invoiceService.assertWarehouseAccess(user, warehouseCode);
        return invoice;
    }

    /** Recomputes the invoice status based on aggregate received vs expected. */
    private void refreshStatus(Invoice invoice) {
        int expected = invoice.getLines().stream().mapToInt(InvoiceLine::getExpectedQuantity).sum();
        int received = invoice.getLines().stream().mapToInt(InvoiceLine::getReceivedQuantity).sum();

        InvoiceStatus status;
        if (received == 0) {
            status = InvoiceStatus.PENDING;
        } else if (received >= expected) {
            status = InvoiceStatus.COMPLETED;
        } else {
            status = InvoiceStatus.IN_PROGRESS;
        }
        if (invoice.getStatus() != status) {
            invoice.setStatus(status);
            invoiceRepository.save(invoice);
        }
    }

    private ScanResultDto buildResult(Invoice invoice, InvoiceLine line) {
        int totalExpected = invoice.getLines().stream().mapToInt(InvoiceLine::getExpectedQuantity).sum();
        int totalReceived = invoice.getLines().stream().mapToInt(InvoiceLine::getReceivedQuantity).sum();

        double linePct = line.getExpectedQuantity() == 0 ? 0
                : Math.min(100.0, (line.getReceivedQuantity() * 100.0) / line.getExpectedQuantity());
        double invPct = totalExpected == 0 ? 0
                : Math.min(100.0, (totalReceived * 100.0) / totalExpected);

        return new ScanResultDto(
                line.getId(), line.getItemSku(), line.getItemName(),
                line.getExpectedQuantity(), line.getReceivedQuantity(), line.getVariance(),
                Math.round(linePct * 10.0) / 10.0,
                totalExpected, totalReceived, Math.round(invPct * 10.0) / 10.0,
                invoice.getStatus().name());
    }
}
