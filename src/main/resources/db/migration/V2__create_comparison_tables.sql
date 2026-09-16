-- 추출·정규화·비교·계산 결과 (docs/db-design.md 3.2)

CREATE TABLE comparison_runs (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    application_id      VARCHAR(64) NOT NULL,
    pre_snapshot_id     BIGINT NOT NULL,
    post_snapshot_id    BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL,
    overall_status      VARCHAR(20) NULL,
    uncertain_reason    VARCHAR(30) NULL,
    total_steps         INT NOT NULL DEFAULT 0,
    completed_steps     INT NOT NULL DEFAULT 0,
    progress_percent    SMALLINT NOT NULL DEFAULT 0,
    policy_version      VARCHAR(32) NOT NULL,
    comparison_hash     CHAR(64) NULL,
    started_at          DATETIME(6) NOT NULL,
    completed_at        DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comparison_runs_snapshots_policy (pre_snapshot_id, post_snapshot_id, policy_version),
    KEY idx_comparison_runs_application_id (application_id),
    CONSTRAINT fk_comparison_runs_pre_snapshot FOREIGN KEY (pre_snapshot_id) REFERENCES condition_pre_snapshots (id),
    CONSTRAINT fk_comparison_runs_post_snapshot FOREIGN KEY (post_snapshot_id) REFERENCES condition_post_snapshots (id),
    CONSTRAINT chk_comparison_runs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_comparison_runs_overall_status
        CHECK (overall_status IS NULL OR overall_status IN ('NO_CHANGE', 'BURDEN_DECREASE', 'CHECK_REQUIRED', 'UNCERTAIN')),
    CONSTRAINT chk_comparison_runs_uncertain_reason
        CHECK (uncertain_reason IS NULL OR uncertain_reason IN ('EXTRACTION_FAILED', 'SOURCE_CONFLICT', 'CALCULATION_UNAVAILABLE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE comparison_run_steps (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id  BIGINT NOT NULL,
    step_code          VARCHAR(40) NOT NULL,
    step_order         TINYINT NOT NULL,
    display_group      VARCHAR(40) NOT NULL,
    status             VARCHAR(15) NOT NULL,
    completed_at       DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comparison_run_steps_run_step (comparison_run_id, step_code),
    CONSTRAINT fk_comparison_run_steps_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id) ON DELETE CASCADE,
    CONSTRAINT chk_comparison_run_steps_status CHECK (status IN ('PENDING', 'DONE', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE comparison_items (
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id        BIGINT NOT NULL,
    field_code               VARCHAR(40) NOT NULL,
    contributes_to_overall   BOOLEAN NOT NULL DEFAULT TRUE,
    item_status              VARCHAR(20) NOT NULL,
    v1_value_text            VARCHAR(200) NULL,
    v2_value_text            VARCHAR(200) NULL,
    v1_value_numeric         DECIMAL(18, 4) NULL,
    v2_value_numeric         DECIMAL(18, 4) NULL,
    unit                     VARCHAR(20) NULL,
    delta_numeric            DECIMAL(18, 4) NULL,
    source                   VARCHAR(15) NOT NULL,
    confidence               DECIMAL(4, 3) NULL,
    unknown_reason           VARCHAR(200) NULL,
    requires_review          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comparison_items_run_field (comparison_run_id, field_code),
    CONSTRAINT fk_comparison_items_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id) ON DELETE CASCADE,
    CONSTRAINT chk_comparison_items_status
        CHECK (item_status IN ('SAME', 'BETTER', 'WORSE', 'STRUCTURAL_CHANGE', 'UNKNOWN')),
    CONSTRAINT chk_comparison_items_source
        CHECK (source IN ('STRUCTURED_API', 'DOCUMENT_AI', 'MANUAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE comparison_item_evidence (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    comparison_item_id    BIGINT NOT NULL,
    reason_code           VARCHAR(40) NULL,
    reason_text           VARCHAR(500) NULL,
    source_document_id    VARCHAR(64) NOT NULL,
    source_page           INT NULL,
    source_span_text      VARCHAR(200) NULL,
    is_required           BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_comparison_item_evidence_item (comparison_item_id),
    CONSTRAINT fk_comparison_item_evidence_item FOREIGN KEY (comparison_item_id) REFERENCES comparison_items (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE comparison_impacts (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id         BIGINT NOT NULL,
    v1_monthly_payment        DECIMAL(14, 0) NOT NULL,
    v2_monthly_payment        DECIMAL(14, 0) NOT NULL,
    monthly_payment_delta     DECIMAL(14, 0) NOT NULL,
    v1_total_interest         DECIMAL(16, 0) NOT NULL,
    v2_total_interest         DECIMAL(16, 0) NOT NULL,
    total_interest_delta      DECIMAL(16, 0) NOT NULL,
    fixed_fee_delta           DECIMAL(14, 0) NOT NULL DEFAULT 0,
    total_cost_delta          DECIMAL(16, 0) NOT NULL,
    calculation_basis         VARCHAR(20) NOT NULL,
    rounding_rule             VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    calculated_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comparison_impacts_run (comparison_run_id),
    CONSTRAINT fk_comparison_impacts_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id) ON DELETE CASCADE,
    CONSTRAINT chk_comparison_impacts_total_cost
        CHECK (total_cost_delta = total_interest_delta + fixed_fee_delta)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
