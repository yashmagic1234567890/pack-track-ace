package com.freshtrack.controller;

import com.freshtrack.dto.*;
import com.freshtrack.service.UserService;
import com.freshtrack.service.WarehouseService;
import com.freshtrack.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Central Admin management endpoints: warehouses, users and warehouse mapping. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTRAL_ADMIN')")
public class AdminController {

    private final WarehouseService warehouseService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    // ----- Warehouses -----

    @PostMapping("/warehouses")
    public ResponseEntity<WarehouseDto> createWarehouse(@Valid @RequestBody CreateWarehouseRequest req) {
        return ResponseEntity.ok(warehouseService.create(req));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseDto>> listWarehouses() {
        return ResponseEntity.ok(warehouseService.listAll());
    }

    // ----- Users -----

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req, securityUtils.currentUsername()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PutMapping("/users/{id}/warehouses")
    public ResponseEntity<UserDto> mapWarehouses(@PathVariable Long id,
                                                 @Valid @RequestBody WarehouseMappingRequest req) {
        return ResponseEntity.ok(
                userService.updateWarehouseMapping(id, req, securityUtils.currentUsername()));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserDto> setStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }
}
