-- Review Gate + 결정 + 전자서명, 무결성 증빙 (docs/db-design.md 3.3, 3.4)

CREATE TABLE decision_required_reviews (
    id                     BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id      BIGINT NOT NULL,
    comparison_item_id     BIGINT NOT NULL,
    reviewed_at            DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_required_reviews_item (comparison_item_id),
    KEY idx_decision_required_reviews_run (comparison_run_id),
    CONSTRAINT fk_decision_required_reviews_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_decision_required_reviews_item FOREIGN KEY (comparison_item_id) REFERENCES comparison_items (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE decisions (
    id                                  BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id                   BIGINT NOT NULL,
    decision_type                       VARCHAR(15) NOT NULL,
    bypassed_uncertain_items            BOOLEAN NOT NULL DEFAULT FALSE,
    all_items_confirmed                 BOOLEAN NOT NULL,
    signature_method                    VARCHAR(20) NOT NULL DEFAULT 'ELECTRONIC',
    signed_at                           DATETIME(6) NULL,
    signature_valid_until               DATETIME(6) NULL,
    post_snapshot_id_at_decision        BIGINT NOT NULL,
    contract_document_hash_at_decision  CHAR(64) NOT NULL,
    created_at                          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_decisions_run (comparison_run_id),
    CONSTRAINT fk_decisions_run FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id),
    CONSTRAINT fk_decisions_post_snapshot FOREIGN KEY (post_snapshot_id_at_decision) REFERENCES condition_post_snapshots (id),
    CONSTRAINT chk_decisions_type CHECK (decision_type IN ('PROCEED', 'RECONSIDER', 'CONSULT')),
    CONSTRAINT chk_decisions_signed_when_proceed
        CHECK (decision_type <> 'PROCEED' OR signed_at IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE proof_records (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    decision_id           BIGINT NOT NULL,
    pre_snapshot_hash     CHAR(64) NOT NULL,
    post_snapshot_hash    CHAR(64) NOT NULL,
    comparison_hash       CHAR(64) NOT NULL,
    decision_hash         CHAR(64) NOT NULL,
    policy_version        VARCHAR(32) NOT NULL,
    proof_payload_hash    CHAR(64) NOT NULL,
    anchor_status         VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    anchor_tx_ref         VARCHAR(128) NULL,
    anchor_retry_count    SMALLINT NOT NULL DEFAULT 0,
    anchored_at           DATETIME(6) NULL,
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_proof_records_decision (decision_id),
    CONSTRAINT fk_proof_records_decision FOREIGN KEY (decision_id) REFERENCES decisions (id),
    CONSTRAINT chk_proof_records_anchor_status CHECK (anchor_status IN ('PENDING', 'ANCHORED', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE proof_verifications (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    proof_record_id      BIGINT NOT NULL,
    requested_by         VARCHAR(20) NOT NULL,
    recomputed_hash      CHAR(64) NOT NULL,
    is_match             BOOLEAN NOT NULL,
    verified_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_proof_verifications_record (proof_record_id),
    CONSTRAINT fk_proof_verifications_record FOREIGN KEY (proof_record_id) REFERENCES proof_records (id),
    CONSTRAINT chk_proof_verifications_requested_by
        CHECK (requested_by IN ('CUSTOMER', 'COUNSELOR', 'COMPLIANCE', 'SYSTEM'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
