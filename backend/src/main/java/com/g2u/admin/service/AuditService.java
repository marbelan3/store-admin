package com.g2u.admin.service;

import com.g2u.admin.domain.audit.AuditLog;
import com.g2u.admin.domain.audit.AuditLogRepository;
import com.g2u.admin.web.dto.AuditLogDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditService self;

    public AuditService(AuditLogRepository auditLogRepository, @Lazy AuditService self) {
        this.auditLogRepository = auditLogRepository;
        this.self = self;
    }

    /**
     * Fire-and-forget audit logging. Exceptions are caught so audit never breaks the main operation.
     */
    public void log(UUID tenantId, UUID userId, String action, String entityType,
                    UUID entityId, Map<String, Object> changes) {
        try {
            self.doLog(tenantId, userId, action, entityType, entityId, changes);
        } catch (Exception e) {
            LOG.warn("Failed to write audit log for {} {} {}: {}", action, entityType, entityId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void doLog(UUID tenantId, UUID userId, String action, String entityType,
                      UUID entityId, Map<String, Object> changes) {
        LOG.debug("Audit: {} {} {} by user {} in tenant {}", action, entityType, entityId, userId, tenantId);

        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .changes(changes)
                .build();

        auditLogRepository.saveAndFlush(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(UUID tenantId, String entityType, UUID entityId,
                                           UUID userId, Instant dateFrom, Instant dateTo,
                                           Pageable pageable) {
        return auditLogRepository.findFiltered(tenantId, entityType, entityId, userId, dateFrom, dateTo, pageable)
                .map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getTenantId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getChanges(),
                auditLog.getCreatedAt()
        );
    }
}
