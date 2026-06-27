package com.freshtrack.dto;

import java.util.List;

/** Successful authentication response containing the JWT and user context. */
public record JwtResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String username,
        String fullName,
        String role,
        List<WarehouseDto> warehouses
) {}
