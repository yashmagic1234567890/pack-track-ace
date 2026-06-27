package com.freshtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Manual adjustment or override of received quantity. {@code delta} may be
 * negative (decrement) or {@code overrideValue} may be set to force an absolute count.
 */
public record ManualAdjustRequest(
        @NotNull Long invoiceId,
        @NotBlank String warehouseCode,
        @NotBlank String itemSku,
        Integer delta,
        Integer overrideValue,
        String reason
) {}
