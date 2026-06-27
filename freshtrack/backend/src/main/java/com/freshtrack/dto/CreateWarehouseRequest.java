package com.freshtrack.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseRequest(
        @NotBlank(message = "Warehouse code is required")
        String warehouseCode,

        @NotBlank(message = "Warehouse name is required")
        String name,

        String location
) {}
