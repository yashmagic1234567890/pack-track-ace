package com.freshtrack.service;

import com.freshtrack.dto.*;
import com.freshtrack.entity.AuditAction;
import com.freshtrack.entity.Role;
import com.freshtrack.entity.User;
import com.freshtrack.repository.UserRepository;
import com.freshtrack.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles authentication and JWT issuance. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final UserService userService;

    @Transactional
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.usernameOrEmail(), request.password()));

        User user = userRepository.findByUsernameOrEmail(
                        request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow();

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());

        auditService.log(AuditAction.LOGIN, user.getUsername(), null, null, null, null, null,
                "User logged in");

        return new JwtResponse(
                token,
                "Bearer",
                tokenProvider.getExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                userService.toWarehouseDtos(user)
        );
    }

    /** Returns the profile of the currently authenticated user. */
    @Transactional(readOnly = true)
    public UserDto currentUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return userService.toDto(user);
    }
}
