package com.freshtrack.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials for login. {@code usernameOrEmail} accepts either field. */
public record LoginRequest(
        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {}
