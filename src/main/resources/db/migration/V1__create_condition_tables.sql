-- V1/V2 조건 Snapshot (docs/db-design.md 3.1)
-- 둘 다 애플리케이션 레벨에서 UPDATE하지 않는다. 재조회/재심사는 항상 새 row.

CREATE TABLE condition_pre_snapshots (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    application_id    VARCHAR(64) NOT NULL,
    customer_id       VARCHAR(64) NOT NULL,
    product_code      VARCHAR(32) NOT NULL,
    product_version   VARCHAR(32) NOT NULL,
    idempotency_key   VARCHAR(100) NOT NULL,
    snapshot_payload  JSON NOT NULL,
    payload_hash      CHAR(64) NOT NULL,
    inquired_at       DATETIME(6) NOT NULL,
    expires_at        DATETIME(6) NOT NULL,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_condition_pre_snapshots_idempotency_key (idempotency_key),
    KEY idx_condition_pre_snapshots_application_id (application_id),
    KEY idx_condition_pre_snapshots_customer_id (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE condition_post_snapshots (
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    application_id           VARCHAR(64) NOT NULL,
    pre_snapshot_id          BIGINT NOT NULL,
    review_version           VARCHAR(32) NOT NULL,
    contract_document_id     VARCHAR(64) NOT NULL,
    contract_document_hash   CHAR(64) NOT NULL,
    snapshot_payload         JSON NOT NULL,
    payload_hash             CHAR(64) NOT NULL,
    decided_at               DATETIME(6) NOT NULL,
    created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_condition_post_snapshots_document (contract_document_id, contract_document_hash),
    KEY idx_condition_post_snapshots_application_id (application_id),
    CONSTRAINT fk_condition_post_snapshots_pre_snapshot
        FOREIGN KEY (pre_snapshot_id) REFERENCES condition_pre_snapshots (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
