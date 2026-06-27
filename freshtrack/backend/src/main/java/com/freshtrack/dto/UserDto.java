package com.freshtrack.dto;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        boolean enabled,
        List<WarehouseDto> warehouses
) {}
