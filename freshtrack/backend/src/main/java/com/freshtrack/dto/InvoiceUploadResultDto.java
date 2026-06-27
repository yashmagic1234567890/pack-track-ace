package com.freshtrack.dto;

import java.util.List;

/** Result of an invoice file ingestion, including any rejected rows. */
public record InvoiceUploadResultDto(
        int invoicesCreated,
        int linesCreated,
        int rowsProcessed,
        int rowsRejected,
        List<String> errors
) {}
