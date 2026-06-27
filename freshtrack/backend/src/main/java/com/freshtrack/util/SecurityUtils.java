package com.freshtrack.util;

import com.freshtrack.entity.User;
import com.freshtrack.exception.ResourceNotFoundException;
import com.freshtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Helper for resolving the currently authenticated {@link User}. */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user in context");
        }
        return auth.getName();
    }

    public User currentUser() {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
