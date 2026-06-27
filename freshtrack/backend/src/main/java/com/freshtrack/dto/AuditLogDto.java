package com.freshtrack.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        String actionType,
        String invoiceBusinessId,
        String itemSku,
        String warehouseCode,
        String username,
        Integer quantityDelta,
        Integer resultingQuantity,
        String details,
        Instant createdAt
) {}
