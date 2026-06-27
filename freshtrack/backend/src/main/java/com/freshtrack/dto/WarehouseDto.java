package com.freshtrack.dto;

/** Lightweight warehouse representation. */
public record WarehouseDto(
        Long id,
        String warehouseCode,
        String name,
        String location
) {}
