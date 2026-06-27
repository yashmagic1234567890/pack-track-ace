package com.freshtrack.service;

import com.freshtrack.dto.*;
import com.freshtrack.entity.*;
import com.freshtrack.exception.AccessDeniedAppException;
import com.freshtrack.exception.BadRequestException;
import com.freshtrack.exception.ResourceNotFoundException;
import com.freshtrack.repository.InvoiceRepository;
import com.freshtrack.repository.WarehouseRepository;
import com.freshtrack.util.InvoiceFileParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/** Central inbound planning: invoice ingestion, listing and detail retrieval. */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final WarehouseRepository warehouseRepository;
    private final InvoiceFileParser parser;
    private final AuditService auditService;

    /**
     * Ingests a CSV/Excel invoice file. Rows sharing an Invoice_ID are grouped
     * into a single invoice with multiple lines. Each Target_Warehouse_ID is
     * validated against existing warehouses before committing.
     */
    @Transactional
    public InvoiceUploadResultDto upload(MultipartFile file, String actor) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided.");
        }

        List<InvoiceFileParser.ParsedRow> rows = parser.parse(file);
        List<String> errors = new ArrayList<>();

        // Group valid rows by Invoice_ID
        Map<String, Invoice> invoiceMap = new LinkedHashMap<>();
        int processed = 0, rejected = 0, linesCreated = 0;

        for (InvoiceFileParser.ParsedRow row : rows) {
            processed++;
            try {
                String invoiceId = require(row, "Invoice_ID");
                String vendor = require(row, "Vendor_Name");
                String whCode = require(row, "Target_Warehouse_ID");
                String sku = require(row, "Item_SKU");
                String itemName = require(row, "Item_Name");
                int qty = parsePositiveInt(row.get("Expected_Quantity"), "Expected_Quantity");

                if (invoiceRepository.existsByInvoiceBusinessId(invoiceId)
                        && !invoiceMap.containsKey(invoiceId)) {
                    throw new BadRequestException("Invoice_ID already exists: " + invoiceId);
                }

                Warehouse warehouse = warehouseRepository.findByWarehouseCode(whCode)
                        .orElseThrow(() -> new BadRequestException(
                                "Target_Warehouse_ID does not exist: " + whCode));

                Invoice invoice = invoiceMap.computeIfAbsent(invoiceId, id -> Invoice.builder()
                        .invoiceBusinessId(id)
                        .vendorName(vendor)
                        .warehouse(warehouse)
                        .status(InvoiceStatus.PENDING)
                        .uploadedBy(actor)
                        .build());

                if (!invoice.getWarehouse().getWarehouseCode().equals(whCode)) {
                    throw new BadRequestException(
                            "Invoice " + invoiceId + " references multiple warehouses.");
                }

                boolean duplicateSku = invoice.getLines().stream()
                        .anyMatch(l -> l.getItemSku().equalsIgnoreCase(sku));
                if (duplicateSku) {
                    throw new BadRequestException("Duplicate SKU " + sku + " in invoice " + invoiceId);
                }

                invoice.addLine(InvoiceLine.builder()
                        .itemSku(sku)
                        .itemName(itemName)
                        .expectedQuantity(qty)
                        .receivedQuantity(0)
                        .build());
                linesCreated++;
            } catch (Exception ex) {
                rejected++;
                errors.add("Row " + row.rowNumber + ": " + ex.getMessage());
            }
        }

        if (invoiceMap.isEmpty()) {
            throw new BadRequestException("No valid invoice rows could be imported. " + errors);
        }

        invoiceRepository.saveAll(invoiceMap.values());

        for (Invoice inv : invoiceMap.values()) {
            auditService.log(AuditAction.INVOICE_UPLOAD, actor, inv.getInvoiceBusinessId(), null,
                    inv.getWarehouse().getWarehouseCode(), inv.getLines().size(), null,
                    "Uploaded invoice with " + inv.getLines().size() + " line(s)");
        }

        return new InvoiceUploadResultDto(
                invoiceMap.size(), linesCreated, processed, rejected, errors);
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDto> listAll() {
        return invoiceRepository.findAll().stream().map(this::toSummary).toList();
    }

    /** Hub User listing — restricted to a warehouse they are authorized for. */
    @Transactional(readOnly = true)
    public List<InvoiceSummaryDto> listForWarehouse(String warehouseCode, User user) {
        assertWarehouseAccess(user, warehouseCode);
        return invoiceRepository.findByWarehouse_WarehouseCode(warehouseCode).stream()
                .map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDetailDto getDetail(Long id, User user) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (user.getRole() == Role.HUB_USER) {
            assertWarehouseAccess(user, invoice.getWarehouse().getWarehouseCode());
        }
        return toDetail(invoice);
    }

    // ----- helpers -----

    public void assertWarehouseAccess(User user, String warehouseCode) {
        if (user.getRole() == Role.CENTRAL_ADMIN) return;
        boolean allowed = user.getWarehouses().stream()
                .anyMatch(w -> w.getWarehouseCode().equals(warehouseCode));
        if (!allowed) {
            throw new AccessDeniedAppException(
                    "You are not authorized to access warehouse " + warehouseCode);
        }
    }

    private String require(InvoiceFileParser.ParsedRow row, String field) {
        String v = row.get(field);
        if (v == null || v.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        return v;
    }

    private int parsePositiveInt(String value, String field) {
        try {
            int n = Integer.parseInt(value.trim());
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new BadRequestException(field + " must be a positive integer (was '" + value + "')");
        }
    }

    public InvoiceSummaryDto toSummary(Invoice i) {
        int expected = i.getLines().stream().mapToInt(InvoiceLine::getExpectedQuantity).sum();
        int received = i.getLines().stream().mapToInt(InvoiceLine::getReceivedQuantity).sum();
        return new InvoiceSummaryDto(
                i.getId(), i.getInvoiceBusinessId(), i.getVendorName(),
                i.getWarehouse().getWarehouseCode(), i.getStatus().name(),
                i.getLines().size(), expected, received, i.getCreatedAt());
    }

    public InvoiceDetailDto toDetail(Invoice i) {
        int expected = i.getLines().stream().mapToInt(InvoiceLine::getExpectedQuantity).sum();
        int received = i.getLines().stream().mapToInt(InvoiceLine::getReceivedQuantity).sum();
        List<InvoiceLineDto> lines = i.getLines().stream()
                .map(this::toLineDto)
                .sorted(Comparator.comparing(InvoiceLineDto::itemName))
                .toList();
        return new InvoiceDetailDto(
                i.getId(), i.getInvoiceBusinessId(), i.getVendorName(),
                i.getWarehouse().getWarehouseCode(), i.getWarehouse().getName(),
                i.getStatus().name(), i.getCreatedAt(), expected, received, lines);
    }

    public InvoiceLineDto toLineDto(InvoiceLine l) {
        double pct = l.getExpectedQuantity() == 0 ? 0
                : Math.min(100.0, (l.getReceivedQuantity() * 100.0) / l.getExpectedQuantity());
        return new InvoiceLineDto(
                l.getId(), l.getItemSku(), l.getItemName(),
                l.getExpectedQuantity(), l.getReceivedQuantity(),
                l.getVariance(), Math.round(pct * 10.0) / 10.0);
    }
}
