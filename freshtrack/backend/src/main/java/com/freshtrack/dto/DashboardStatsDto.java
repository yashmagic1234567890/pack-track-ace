package com.freshtrack.dto;

import java.util.List;

/** Aggregated KPIs for the dashboard / analytics view. */
public record DashboardStatsDto(
        long totalInvoices,
        long pendingInvoices,
        long inProgressInvoices,
        long completedInvoices,
        long totalWarehouses,
        long totalHubUsers,
        long totalExpectedUnits,
        long totalReceivedUnits,
        long totalVariance,
        double overallProgressPercent,
        List<WarehouseStat> perWarehouse,
        List<VendorStat> topVendors
) {
    public record WarehouseStat(String warehouseCode, String name,
                                long expected, long received, long variance) {}

    public record VendorStat(String vendorName, long expected, long received, long variance) {}
}
