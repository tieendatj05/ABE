package com.abe.system.abe_system.dto.audit;

import com.abe.system.abe_system.entity.AuditAction;
import com.abe.system.abe_system.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private Long fileId;
    private String fileName;
    private Long ownerId;
    private String ownerUsername;
    private Long requesterId;
    private String requesterUsername;
    private String accessPolicy;
    private String detail;
    private LocalDateTime occurredAt;

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getFileId(),
                log.getFileName(),
                log.getOwnerId(),
                log.getOwnerUsername(),
                log.getRequesterId(),
                log.getRequesterUsername(),
                log.getAccessPolicy(),
                log.getDetail(),
                log.getOccurredAt()
        );
    }
}
