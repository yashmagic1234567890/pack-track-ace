package com.freshtrack.dto;

/** Real-time progress feedback returned after a scan or manual adjustment. */
public record ScanResultDto(
        Long lineId,
        String itemSku,
        String itemName,
        int expectedQuantity,
        int receivedQuantity,
        int variance,
        double lineProgressPercent,
        int invoiceTotalExpected,
        int invoiceTotalReceived,
        double invoiceProgressPercent,
        String invoiceStatus
) {}
