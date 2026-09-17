-- docs/implementation-spec.md 13장(Blockchain Adapter Contract) 반영.
-- comparison_runs: 단일 policy_version -> prompt/rule/calculation version 3분리
-- proof_records / proof_verifications: Canonical Proof Payload 필드에 맞춰 재작성

ALTER TABLE comparison_runs
    DROP COLUMN policy_version,
    ADD COLUMN prompt_version      VARCHAR(64) NULL AFTER completed_at,
    ADD COLUMN rule_version        VARCHAR(32) NOT NULL AFTER prompt_version,
    ADD COLUMN calculation_version VARCHAR(32) NOT NULL AFTER rule_version;

ALTER TABLE comparison_runs
    ADD UNIQUE KEY uk_comparison_runs_snapshots_versions
        (pre_snapshot_id, post_snapshot_id, rule_version, calculation_version);

-- 기존 uk_comparison_runs_snapshots_policy(V2에서 생성)는 더 이상 쓰지 않으므로 제거
ALTER TABLE comparison_runs
    DROP INDEX uk_comparison_runs_snapshots_policy;

DROP TABLE proof_verifications;
DROP TABLE proof_records;

CREATE TABLE proof_records (
    id                             BIGINT NOT NULL AUTO_INCREMENT,
    decision_id                    BIGINT NOT NULL,
    proof_id                       VARCHAR(64) NOT NULL,
    record_id_hash                 CHAR(64) NOT NULL,
    schema_version                 VARCHAR(10) NOT NULL DEFAULT '1.0',
    check_id_hash                  CHAR(64) NOT NULL,
    application_id_hash            CHAR(64) NOT NULL,
    v1_snapshot_hash                CHAR(64) NOT NULL,
    v2_snapshot_hash                CHAR(64) NOT NULL,
    comparison_result_hash          CHAR(64) NOT NULL,
    decision_hash                   CHAR(64) NOT NULL,
    issuer_id                      VARCHAR(64) NOT NULL,
    prompt_version                 VARCHAR(64) NULL,
    rule_version                   VARCHAR(32) NOT NULL,
    calculation_version             VARCHAR(32) NOT NULL,
    payload_hash                   CHAR(64) NOT NULL,
    supersedes_record_id_hash       CHAR(64) NULL,
    anchor_status                  VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    ledger_reference                VARCHAR(128) NULL,
    confirmation_count              INT NOT NULL DEFAULT 0,
    submitted_at                   DATETIME(6) NULL,
    confirmed_at                   DATETIME(6) NULL,
    anchor_retry_count              SMALLINT NOT NULL DEFAULT 0,
    next_retry_at                  DATETIME(6) NULL,
    created_at                     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_proof_records_decision (decision_id),
    UNIQUE KEY uk_proof_records_proof_id (proof_id),
    UNIQUE KEY uk_proof_records_record_id_hash (record_id_hash),
    CONSTRAINT fk_proof_records_decision FOREIGN KEY (decision_id) REFERENCES decisions (id),
    CONSTRAINT chk_proof_records_anchor_status
        CHECK (anchor_status IN ('PENDING', 'SUBMITTED', 'CONFIRMED', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE proof_verifications (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    proof_record_id      BIGINT NOT NULL,
    requested_by         VARCHAR(20) NOT NULL,
    recomputed_hash      CHAR(64) NOT NULL,
    verify_result        VARCHAR(20) NOT NULL,
    ledger_recorded_at   DATETIME(6) NULL,
    verified_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_proof_verifications_record (proof_record_id),
    CONSTRAINT fk_proof_verifications_record FOREIGN KEY (proof_record_id) REFERENCES proof_records (id),
    CONSTRAINT chk_proof_verifications_requested_by
        CHECK (requested_by IN ('CUSTOMER', 'COUNSELOR', 'COMPLIANCE', 'SYSTEM')),
    CONSTRAINT chk_proof_verifications_result
        CHECK (verify_result IN ('VERIFIED', 'NOT_ANCHORED', 'HASH_MISMATCH'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
