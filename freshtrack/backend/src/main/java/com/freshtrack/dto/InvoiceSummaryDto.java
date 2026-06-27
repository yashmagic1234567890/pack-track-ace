package com.freshtrack.dto;

import java.time.Instant;

/** Summary row for invoice lists. */
public record InvoiceSummaryDto(
        Long id,
        String invoiceBusinessId,
        String vendorName,
        String warehouseCode,
        String status,
        int totalLines,
        int totalExpected,
        int totalReceived,
        Instant createdAt
) {}
