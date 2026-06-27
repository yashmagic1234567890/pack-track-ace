package com.freshtrack.service;

import com.freshtrack.dto.AuditLogDto;
import com.freshtrack.entity.AuditAction;
import com.freshtrack.entity.AuditLog;
import com.freshtrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Centralised, append-only audit logging. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> list(String invoiceBusinessId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        Page<AuditLog> result = (invoiceBusinessId == null || invoiceBusinessId.isBlank())
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pr)
                : auditLogRepository.findByInvoiceBusinessIdOrderByCreatedAtDesc(invoiceBusinessId, pr);
        return result.map(this::toDto);
    }

    public AuditLogDto toDto(AuditLog a) {
        return new AuditLogDto(
                a.getId(), a.getActionType().name(), a.getInvoiceBusinessId(),
                a.getItemSku(), a.getWarehouseCode(), a.getUsername(),
                a.getQuantityDelta(), a.getResultingQuantity(), a.getDetails(), a.getCreatedAt());
    }


    /**
     * Records an audit entry in its own transaction so the audit trail is never
     * lost even if the surrounding business transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String username, String invoiceId, String sku,
                    String warehouseCode, Integer delta, Integer resulting, String details) {
        AuditLog entry = AuditLog.builder()
                .actionType(action)
                .username(username)
                .invoiceBusinessId(invoiceId)
                .itemSku(sku)
                .warehouseCode(warehouseCode)
                .quantityDelta(delta)
                .resultingQuantity(resulting)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}
