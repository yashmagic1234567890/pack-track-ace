package com.freshtrack.controller;

import com.freshtrack.dto.ManualAdjustRequest;
import com.freshtrack.dto.ScanRequest;
import com.freshtrack.dto.ScanResultDto;
import com.freshtrack.service.ReceivingService;
import com.freshtrack.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Scan-to-Receive endpoints used by Hub Users on the warehouse floor. */
@RestController
@RequestMapping("/api/receiving")
@RequiredArgsConstructor
public class ReceivingController {

    private final ReceivingService receivingService;
    private final SecurityUtils securityUtils;

    /** Barcode/camera scan: increments the matching SKU's received qty by +1. */
    @PostMapping("/scan")
    public ResponseEntity<ScanResultDto> scan(@Valid @RequestBody ScanRequest req) {
        return ResponseEntity.ok(receivingService.scan(req, securityUtils.currentUser()));
    }

    /** Manual increment/decrement or absolute override of a received quantity. */
    @PostMapping("/adjust")
    public ResponseEntity<ScanResultDto> adjust(@Valid @RequestBody ManualAdjustRequest req) {
        return ResponseEntity.ok(receivingService.adjust(req, securityUtils.currentUser()));
    }
}
