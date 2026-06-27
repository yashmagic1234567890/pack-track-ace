package com.freshtrack.service;

import com.freshtrack.dto.ReconciliationRowDto;
import com.freshtrack.entity.AuditAction;
import com.freshtrack.entity.Invoice;
import com.freshtrack.entity.InvoiceLine;
import com.freshtrack.entity.Role;
import com.freshtrack.entity.User;
import com.freshtrack.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Builds the reconciliation report (Variance = Expected - Received) and exports it. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String[] HEADERS = {
            "Invoice_ID", "Vendor_Name", "Target_Warehouse_ID",
            "Item_SKU", "Item_Name", "Expected_Quantity", "Received_Quantity", "Variance"
    };

    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ReconciliationRowDto> reconcile(String warehouseCode, String vendorName,
                                                Instant from, Instant to, User user) {
        // Hub users are constrained to their mapped warehouses.
        String effectiveWarehouse = warehouseCode;
        List<ReconciliationRowDto> rows = new ArrayList<>();

        List<Invoice> invoices = invoiceRepository.filterForReconciliation(
                effectiveWarehouse, vendorName, from, to);

        for (Invoice inv : invoices) {
            if (user.getRole() == Role.HUB_USER) {
                boolean allowed = user.getWarehouses().stream()
                        .anyMatch(w -> w.getWarehouseCode().equals(inv.getWarehouse().getWarehouseCode()));
                if (!allowed) continue;
            }
            for (InvoiceLine l : inv.getLines()) {
                rows.add(new ReconciliationRowDto(
                        inv.getInvoiceBusinessId(), inv.getVendorName(),
                        inv.getWarehouse().getWarehouseCode(), l.getItemSku(), l.getItemName(),
                        l.getExpectedQuantity(), l.getReceivedQuantity(), l.getVariance()));
            }
        }
        return rows;
    }

    public byte[] exportCsv(List<ReconciliationRowDto> rows, String actor) {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw,
                CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (ReconciliationRowDto r : rows) {
                printer.printRecord(r.invoiceId(), r.vendorName(), r.warehouseId(),
                        r.itemSku(), r.itemName(), r.expectedQuantity(),
                        r.receivedQuantity(), r.variance());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
        auditService.log(AuditAction.REPORT_EXPORT, actor, null, null, null, rows.size(), null,
                "Exported reconciliation report (CSV)");
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel(List<ReconciliationRowDto> rows, String actor) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Reconciliation");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            int r = 1;
            for (ReconciliationRowDto row : rows) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(row.invoiceId());
                excelRow.createCell(1).setCellValue(row.vendorName());
                excelRow.createCell(2).setCellValue(row.warehouseId());
                excelRow.createCell(3).setCellValue(row.itemSku());
                excelRow.createCell(4).setCellValue(row.itemName());
                excelRow.createCell(5).setCellValue(row.expectedQuantity());
                excelRow.createCell(6).setCellValue(row.receivedQuantity());
                excelRow.createCell(7).setCellValue(row.variance());
            }
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            auditService.log(AuditAction.REPORT_EXPORT, actor, null, null, null, rows.size(), null,
                    "Exported reconciliation report (Excel)");
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}
