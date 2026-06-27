package com.freshtrack.controller;

import com.freshtrack.dto.DashboardStatsDto;
import com.freshtrack.dto.WarehouseDto;
import com.freshtrack.entity.Role;
import com.freshtrack.entity.User;
import com.freshtrack.service.DashboardService;
import com.freshtrack.service.UserService;
import com.freshtrack.service.WarehouseService;
import com.freshtrack.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dashboard / analytics KPIs scoped to the caller's role and warehouses. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final WarehouseService warehouseService;
    private final SecurityUtils securityUtils;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> stats() {
        return ResponseEntity.ok(dashboardService.getStats(securityUtils.currentUser()));
    }

    /** Warehouses the current user can operate in (for selection dropdowns). */
    @GetMapping("/my-warehouses")
    public ResponseEntity<List<WarehouseDto>> myWarehouses() {
        User user = securityUtils.currentUser();
        if (user.getRole() == Role.CENTRAL_ADMIN) {
            return ResponseEntity.ok(warehouseService.listAll());
        }
        return ResponseEntity.ok(userService.toWarehouseDtos(user));
    }
}

