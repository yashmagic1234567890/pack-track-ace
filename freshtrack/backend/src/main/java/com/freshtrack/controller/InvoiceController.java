package com.freshtrack.controller;

import com.freshtrack.dto.InvoiceDetailDto;
import com.freshtrack.dto.InvoiceSummaryDto;
import com.freshtrack.dto.InvoiceUploadResultDto;
import com.freshtrack.service.InvoiceService;
import com.freshtrack.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Invoice ingestion (admin) and querying (admin + hub users). */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final SecurityUtils securityUtils;

    /** CSV/Excel ingestion — Central Admin only. */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
    public ResponseEntity<InvoiceUploadResultDto> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(invoiceService.upload(file, securityUtils.currentUsername()));
    }

    /** All invoices — Central Admin only. */
    @GetMapping
    @PreAuthorize("hasRole('CENTRAL_ADMIN')")
    public ResponseEntity<List<InvoiceSummaryDto>> listAll() {
        return ResponseEntity.ok(invoiceService.listAll());
    }

    /** Invoices for a warehouse — accessible to mapped Hub Users and Admins. */
    @GetMapping("/by-warehouse/{warehouseCode}")
    public ResponseEntity<List<InvoiceSummaryDto>> byWarehouse(@PathVariable String warehouseCode) {
        return ResponseEntity.ok(
                invoiceService.listForWarehouse(warehouseCode, securityUtils.currentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getDetail(id, securityUtils.currentUser()));
    }
}
