package com.freshtrack.controller;

import com.freshtrack.dto.ReconciliationRowDto;
import com.freshtrack.entity.User;
import com.freshtrack.service.ReportService;
import com.freshtrack.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** Reconciliation report viewing and CSV/Excel export. */
@RestController
@RequestMapping("/api/reports/reconciliation")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<ReconciliationRowDto>> view(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        User user = securityUtils.currentUser();
        return ResponseEntity.ok(reportService.reconcile(warehouseCode, vendorName, from, to, user));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        User user = securityUtils.currentUser();
        List<ReconciliationRowDto> rows = reportService.reconcile(warehouseCode, vendorName, from, to, user);
        byte[] data = reportService.exportCsv(rows, user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reconciliation.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        User user = securityUtils.currentUser();
        List<ReconciliationRowDto> rows = reportService.reconcile(warehouseCode, vendorName, from, to, user);
        byte[] data = reportService.exportExcel(rows, user.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reconciliation.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
