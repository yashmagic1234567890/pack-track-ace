package com.freshtrack.service;

import com.freshtrack.dto.DashboardStatsDto;
import com.freshtrack.entity.*;
import com.freshtrack.repository.InvoiceRepository;
import com.freshtrack.repository.UserRepository;
import com.freshtrack.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Aggregates KPIs for the dashboard and analytics views. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats(User user) {
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> user.getRole() == Role.CENTRAL_ADMIN
                        || user.getWarehouses().stream()
                        .anyMatch(w -> w.getWarehouseCode()
                                .equals(inv.getWarehouse().getWarehouseCode())))
                .toList();

        long pending = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PENDING).count();
        long inProgress = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.IN_PROGRESS).count();
        long completed = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.COMPLETED).count();

        long expected = invoices.stream().flatMap(i -> i.getLines().stream())
                .mapToLong(InvoiceLine::getExpectedQuantity).sum();
        long received = invoices.stream().flatMap(i -> i.getLines().stream())
                .mapToLong(InvoiceLine::getReceivedQuantity).sum();

        Map<String, long[]> byWarehouse = new LinkedHashMap<>();
        Map<String, String> warehouseNames = new HashMap<>();
        Map<String, long[]> byVendor = new LinkedHashMap<>();

        for (Invoice inv : invoices) {
            String wc = inv.getWarehouse().getWarehouseCode();
            warehouseNames.put(wc, inv.getWarehouse().getName());
            long[] wAgg = byWarehouse.computeIfAbsent(wc, k -> new long[2]);
            long[] vAgg = byVendor.computeIfAbsent(inv.getVendorName(), k -> new long[2]);
            for (InvoiceLine l : inv.getLines()) {
                wAgg[0] += l.getExpectedQuantity();
                wAgg[1] += l.getReceivedQuantity();
                vAgg[0] += l.getExpectedQuantity();
                vAgg[1] += l.getReceivedQuantity();
            }
        }

        List<DashboardStatsDto.WarehouseStat> perWarehouse = byWarehouse.entrySet().stream()
                .map(e -> new DashboardStatsDto.WarehouseStat(
                        e.getKey(), warehouseNames.get(e.getKey()),
                        e.getValue()[0], e.getValue()[1], e.getValue()[0] - e.getValue()[1]))
                .sorted(Comparator.comparingLong(DashboardStatsDto.WarehouseStat::expected).reversed())
                .collect(Collectors.toList());

        List<DashboardStatsDto.VendorStat> topVendors = byVendor.entrySet().stream()
                .map(e -> new DashboardStatsDto.VendorStat(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[0] - e.getValue()[1]))
                .sorted(Comparator.comparingLong(DashboardStatsDto.VendorStat::expected).reversed())
                .limit(5)
                .collect(Collectors.toList());

        double overall = expected == 0 ? 0 : Math.min(100.0, (received * 100.0) / expected);
        long totalWarehouses = user.getRole() == Role.CENTRAL_ADMIN
                ? warehouseRepository.count() : user.getWarehouses().size();
        long hubUsers = user.getRole() == Role.CENTRAL_ADMIN
                ? userRepository.findAll().stream().filter(u -> u.getRole() == Role.HUB_USER).count() : 0;

        return new DashboardStatsDto(
                invoices.size(), pending, inProgress, completed,
                totalWarehouses, hubUsers, expected, received, expected - received,
                Math.round(overall * 10.0) / 10.0, perWarehouse, topVendors);
    }
}
