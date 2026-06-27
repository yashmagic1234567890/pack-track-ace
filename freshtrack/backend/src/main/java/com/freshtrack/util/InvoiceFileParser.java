package com.freshtrack.util;

import com.freshtrack.exception.BadRequestException;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses uploaded invoice files (CSV or XLSX) into a uniform list of row maps,
 * keyed by the canonical header names defined in the BRD.
 */
@Component
public class InvoiceFileParser {

    /** Canonical headers expected in the invoice file. */
    public static final List<String> REQUIRED_HEADERS = List.of(
            "Invoice_ID", "Vendor_Name", "Target_Warehouse_ID",
            "Item_SKU", "Item_Name", "Expected_Quantity");

    @Getter
    public static class ParsedRow {
        public final int rowNumber;
        public final Map<String, String> values;
        public ParsedRow(int rowNumber, Map<String, String> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }
        public String get(String key) {
            String v = values.get(key);
            return v == null ? "" : v.trim();
        }
    }

    public List<ParsedRow> parse(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try (InputStream is = file.getInputStream()) {
            if (name.endsWith(".csv")) {
                return parseCsv(is);
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return parseExcel(is);
            }
            throw new BadRequestException("Unsupported file type. Upload a .csv or .xlsx file.");
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    private List<ParsedRow> parseCsv(InputStream is) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setTrim(true)
                .setIgnoreEmptyLines(true).build()
                .parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))) {

            validateHeaders(parser.getHeaderNames());
            int n = 1;
            for (CSVRecord rec : parser) {
                n++;
                Map<String, String> map = new LinkedHashMap<>();
                for (String h : REQUIRED_HEADERS) {
                    map.put(h, rec.isMapped(h) ? rec.get(h) : "");
                }
                rows.add(new ParsedRow(n, map));
            }
        }
        return rows;
    }

    private List<ParsedRow> parseExcel(InputStream is) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new BadRequestException("The uploaded spreadsheet is empty.");
            }
            DataFormatter fmt = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<Integer, String> colIndex = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getColumnIndex(), fmt.formatCellValue(cell).trim());
            }
            validateHeaders(colIndex.values().stream().toList());

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean blank = true;
                for (Map.Entry<Integer, String> e : colIndex.entrySet()) {
                    Cell c = row.getCell(e.getKey());
                    String val = c == null ? "" : fmt.formatCellValue(c).trim();
                    if (!val.isEmpty()) blank = false;
                    map.put(e.getValue(), val);
                }
                if (!blank) rows.add(new ParsedRow(r + 1, map));
            }
        }
        return rows;
    }

    private void validateHeaders(List<String> headers) {
        for (String required : REQUIRED_HEADERS) {
            if (headers.stream().noneMatch(h -> h.equalsIgnoreCase(required))) {
                throw new BadRequestException(
                        "Missing required column '" + required + "'. Expected columns: " + REQUIRED_HEADERS);
            }
        }
    }
}
