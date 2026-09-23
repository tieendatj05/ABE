package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByOccurredAtDesc();

    List<AuditLog> findByOwnerIdOrderByOccurredAtDesc(Long ownerId);

    List<AuditLog> findByFileIdOrderByOccurredAtDesc(Long fileId);
}
