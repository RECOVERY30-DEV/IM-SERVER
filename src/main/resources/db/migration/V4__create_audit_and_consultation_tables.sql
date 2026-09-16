-- 상담 핸드오프(경량) + append-only 감사 이벤트 (docs/db-design.md 3.5, 3.6)

CREATE TABLE consultation_referrals (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id           BIGINT NOT NULL,
    application_id              VARCHAR(64) NOT NULL,
    transfer_consent_granted    BOOLEAN NOT NULL DEFAULT FALSE,
    context_snapshot            JSON NULL,
    external_consultation_ref   VARCHAR(64) NULL,
    requested_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_consultation_referrals_run (comparison_run_id),
    KEY idx_consultation_referrals_application (application_id),
    CONSTRAINT fk_consultation_referrals_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE audit_condition_events (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    event_type        VARCHAR(30) NOT NULL,
    application_id    VARCHAR(64) NOT NULL,
    actor_type        VARCHAR(15) NOT NULL,
    ref_type          VARCHAR(30) NOT NULL,
    ref_id            BIGINT NOT NULL,
    event_payload     JSON NULL,
    occurred_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_condition_events_application_occurred (application_id, occurred_at),
    CONSTRAINT chk_audit_condition_events_type CHECK (event_type IN (
        'V1_SAVED', 'V2_SAVED', 'COMPARISON_STARTED', 'COMPARISON_COMPLETED',
        'ITEM_REVIEWED', 'DECISION_SUBMITTED', 'PROOF_ANCHORED', 'PROOF_VERIFIED'
    )),
    CONSTRAINT chk_audit_condition_events_actor CHECK (actor_type IN ('CUSTOMER', 'COUNSELOR', 'SYSTEM'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
