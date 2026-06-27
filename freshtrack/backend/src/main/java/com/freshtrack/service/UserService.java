package com.freshtrack.service;

import com.freshtrack.dto.*;
import com.freshtrack.entity.AuditAction;
import com.freshtrack.entity.Role;
import com.freshtrack.entity.User;
import com.freshtrack.entity.Warehouse;
import com.freshtrack.exception.BadRequestException;
import com.freshtrack.exception.ResourceNotFoundException;
import com.freshtrack.repository.UserRepository;
import com.freshtrack.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** User management and user-to-warehouse mapping (Central Admin operations). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserDto createUser(CreateUserRequest req, String actor) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BadRequestException("Username already exists: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already exists: " + req.email());
        }

        Role role;
        try {
            role = Role.valueOf(req.role());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role. Use CENTRAL_ADMIN or HUB_USER.");
        }

        Set<Warehouse> warehouses = resolveWarehouses(req.warehouseCodes());

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(role)
                .enabled(true)
                .warehouses(warehouses)
                .build();

        user = userRepository.save(user);
        auditService.log(AuditAction.USER_CREATED, actor, null, null, null, null, null,
                "Created user " + user.getUsername() + " (" + role + ")");
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public UserDto updateWarehouseMapping(Long userId, WarehouseMappingRequest req, String actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setWarehouses(resolveWarehouses(req.warehouseCodes()));
        user = userRepository.save(user);
        auditService.log(AuditAction.WAREHOUSE_MAPPING_UPDATED, actor, null, null, null, null, null,
                "Updated warehouse mapping for " + user.getUsername() + " -> " + req.warehouseCodes());
        return toDto(user);
    }

    @Transactional
    public UserDto setEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setEnabled(enabled);
        return toDto(userRepository.save(user));
    }

    private Set<Warehouse> resolveWarehouses(Set<String> codes) {
        Set<Warehouse> result = new HashSet<>();
        if (codes == null) return result;
        for (String code : codes) {
            Warehouse w = warehouseRepository.findByWarehouseCode(code)
                    .orElseThrow(() -> new BadRequestException("Warehouse code does not exist: " + code));
            result.add(w);
        }
        return result;
    }

    // ----- mappers -----

    public UserDto toDto(User u) {
        return new UserDto(
                u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.isEnabled(), toWarehouseDtos(u));
    }

    public List<WarehouseDto> toWarehouseDtos(User u) {
        return u.getWarehouses().stream()
                .map(w -> new WarehouseDto(w.getId(), w.getWarehouseCode(), w.getName(), w.getLocation()))
                .toList();
    }
}
