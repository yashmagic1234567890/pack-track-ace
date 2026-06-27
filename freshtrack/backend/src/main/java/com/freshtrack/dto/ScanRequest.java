package com.freshtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A single scan event. The Hub User must be operating within {@code warehouseCode}
 * (enforced server-side for data isolation). Scanning increments received qty by +1.
 */
public record ScanRequest(
        @NotNull(message = "invoiceId is required")
        Long invoiceId,

        @NotBlank(message = "warehouseCode is required")
        String warehouseCode,

        @NotBlank(message = "scanned SKU is required")
        String itemSku
) {}
