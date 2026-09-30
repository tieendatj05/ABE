-- Constraint nay do Hibernate tu sinh TRUOC KHI co Flyway (con dung
-- ddl-auto=update) cho cot enum AuditAction.action, liet ke dung 4 gia tri
-- ton tai luc do - GIONG HET van de users_role_check da biet truoc day.
-- Hibernate "update" mode khong tu ALTER lai constraint khi Java enum co them
-- gia tri moi (INTEGRITY_VIOLATION) - phai tu cap nhat bang tay o day.
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN ('UPLOAD', 'DOWNLOAD_SUCCESS', 'DOWNLOAD_DENIED', 'DELETE', 'INTEGRITY_VIOLATION'));
