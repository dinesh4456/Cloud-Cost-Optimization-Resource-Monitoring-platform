CREATE TABLE scan_jobs (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    triggered_by_id            BIGINT NULL,
    status                     VARCHAR(20)  NOT NULL,
    region                     VARCHAR(40)  NOT NULL,
    error_message              VARCHAR(2000) NULL,
    resources_scanned_count    INT          NOT NULL DEFAULT 0,
    health_score               INT          NULL,
    estimated_monthly_cost     DECIMAL(12, 2) NULL,
    estimated_monthly_savings  DECIMAL(12, 2) NULL,
    started_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at               TIMESTAMP    NULL,
    CONSTRAINT fk_scan_jobs_user FOREIGN KEY (triggered_by_id) REFERENCES users (id),
    CONSTRAINT ck_scan_jobs_status CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_snapshots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_job_id     BIGINT       NOT NULL,
    resource_type   VARCHAR(20)  NOT NULL,
    resource_id     VARCHAR(255) NOT NULL,
    resource_name   VARCHAR(255) NULL,
    region          VARCHAR(40)  NOT NULL,
    state           VARCHAR(80)  NULL,
    cpu_avg_7d      DECIMAL(8, 2) NULL,
    attributes_json TEXT         NULL,
    last_seen_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshots_scan FOREIGN KEY (scan_job_id) REFERENCES scan_jobs (id) ON DELETE CASCADE,
    CONSTRAINT ck_snapshots_type CHECK (resource_type IN ('EC2', 'EBS', 'EIP', 'S3', 'IAM')),
    INDEX idx_snapshots_scan_type (scan_job_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resource_metrics (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id   BIGINT         NOT NULL,
    metric_name   VARCHAR(80)    NOT NULL,
    metric_value  DECIMAL(18, 4) NOT NULL,
    unit          VARCHAR(40)    NOT NULL,
    period_start  TIMESTAMP      NOT NULL,
    period_end    TIMESTAMP      NOT NULL,
    CONSTRAINT fk_metrics_snapshot FOREIGN KEY (snapshot_id) REFERENCES resource_snapshots (id) ON DELETE CASCADE,
    INDEX idx_metrics_snapshot (snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recommendations (
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_job_id                BIGINT         NOT NULL,
    snapshot_id                BIGINT         NULL,
    rule_code                  VARCHAR(40)    NOT NULL,
    title                      VARCHAR(200)   NOT NULL,
    description                VARCHAR(2000)  NOT NULL,
    severity                   VARCHAR(20)    NOT NULL,
    estimated_monthly_savings  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status                     VARCHAR(20)    NOT NULL,
    created_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reco_scan FOREIGN KEY (scan_job_id) REFERENCES scan_jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_reco_snapshot FOREIGN KEY (snapshot_id) REFERENCES resource_snapshots (id) ON DELETE SET NULL,
    CONSTRAINT ck_reco_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_reco_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'DISMISSED')),
    INDEX idx_reco_scan (scan_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alerts (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_job_id        BIGINT        NOT NULL,
    recommendation_id  BIGINT        NULL,
    alert_type         VARCHAR(40)   NOT NULL,
    severity           VARCHAR(20)   NOT NULL,
    message            VARCHAR(500)  NOT NULL,
    is_read            TINYINT(1)    NOT NULL DEFAULT 0,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_scan FOREIGN KEY (scan_job_id) REFERENCES scan_jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_reco FOREIGN KEY (recommendation_id) REFERENCES recommendations (id) ON DELETE SET NULL,
    CONSTRAINT ck_alerts_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    generated_by_id BIGINT        NOT NULL,
    report_type     VARCHAR(20)   NOT NULL,
    period_start    TIMESTAMP     NOT NULL,
    period_end      TIMESTAMP     NOT NULL,
    file_path       VARCHAR(500)  NOT NULL,
    summary_json    TEXT          NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_user FOREIGN KEY (generated_by_id) REFERENCES users (id),
    CONSTRAINT ck_reports_type CHECK (report_type IN ('WEEKLY', 'MONTHLY', 'OPTIMIZATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE activity_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NULL,
    action_type  VARCHAR(40)   NOT NULL,
    entity_type  VARCHAR(40)   NULL,
    entity_id    VARCHAR(80)   NULL,
    details      VARCHAR(1000) NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_activity_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
