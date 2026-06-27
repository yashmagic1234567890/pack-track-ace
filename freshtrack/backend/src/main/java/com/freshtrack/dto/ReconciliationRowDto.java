package com.freshtrack.dto;

/** A single row of the reconciliation report. */
public record ReconciliationRowDto(
        String invoiceId,
        String vendorName,
        String warehouseId,
        String itemSku,
        String itemName,
        int expectedQuantity,
        int receivedQuantity,
        int variance
) {}
