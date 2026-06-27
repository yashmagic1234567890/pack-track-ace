package com.freshtrack.controller;

import com.freshtrack.dto.JwtResponse;
import com.freshtrack.dto.LoginRequest;
import com.freshtrack.dto.UserDto;
import com.freshtrack.service.AuthService;
import com.freshtrack.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Authentication endpoints (public login + authenticated profile). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.currentUser(securityUtils.currentUsername()));
    }
}
