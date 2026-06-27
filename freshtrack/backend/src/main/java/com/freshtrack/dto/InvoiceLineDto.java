package com.freshtrack.dto;

public record InvoiceLineDto(
        Long id,
        String itemSku,
        String itemName,
        int expectedQuantity,
        int receivedQuantity,
        int variance,
        double progressPercent
) {}
