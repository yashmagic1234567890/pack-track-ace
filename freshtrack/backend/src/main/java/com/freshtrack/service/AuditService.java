package com.freshtrack.service;

import com.freshtrack.entity.AuditAction;
import com.freshtrack.entity.AuditLog;
import com.freshtrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Centralised, append-only audit logging. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

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
