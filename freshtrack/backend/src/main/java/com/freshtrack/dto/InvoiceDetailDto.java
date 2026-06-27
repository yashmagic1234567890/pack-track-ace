package com.freshtrack.dto;

import java.time.Instant;
import java.util.List;

public record InvoiceDetailDto(
        Long id,
        String invoiceBusinessId,
        String vendorName,
        String warehouseCode,
        String warehouseName,
        String status,
        Instant createdAt,
        int totalExpected,
        int totalReceived,
        List<InvoiceLineDto> lines
) {}
