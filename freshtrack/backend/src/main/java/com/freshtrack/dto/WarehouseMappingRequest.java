package com.freshtrack.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/** Replace the set of warehouses mapped to a Hub User. */
public record WarehouseMappingRequest(
        @NotEmpty(message = "At least one warehouse code is required")
        Set<String> warehouseCodes
) {}
