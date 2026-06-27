package com.freshtrack.controller;

import com.freshtrack.dto.AuditLogDto;
import com.freshtrack.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Audit trail viewing — Central Admin only. */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CENTRAL_ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> list(
            @RequestParam(required = false) String invoiceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.list(invoiceId, page, size));
    }
}
