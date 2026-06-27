package com.freshtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Central Admin creates a user and optionally maps warehouses up-front. */
public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank @Email(message = "A valid email is required")
        String email,

        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String fullName,

        @NotNull(message = "Role is required (CENTRAL_ADMIN or HUB_USER)")
        String role,

        /** Warehouse codes to map (HUB_USER). */
        Set<String> warehouseCodes
) {}
