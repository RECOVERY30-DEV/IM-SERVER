-- implementation-spec.md 13.4 BlockchainAdapter의 기본(Mock) 구현이 쓰는 "원장" 저장소.
-- 실제 체인/SDK가 정해지면 이 테이블 대신 그 네트워크를 호출하는 어댑터로 교체한다 —
-- proof 모듈의 다른 코드는 BlockchainAdapter 인터페이스만 알고 이 테이블 존재 자체를 모른다.
CREATE TABLE proof_mock_ledger (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    record_id_hash              CHAR(64) NOT NULL,
    payload_hash                CHAR(64) NOT NULL,
    schema_version              VARCHAR(10) NOT NULL,
    issuer_id                   VARCHAR(64) NOT NULL,
    supersedes_record_id_hash   CHAR(64) NULL,
    ledger_reference            VARCHAR(128) NOT NULL,
    confirmation_count          INT NOT NULL DEFAULT 1,
    recorded_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_proof_mock_ledger_record_id_hash (record_id_hash),
    UNIQUE KEY uk_proof_mock_ledger_reference (ledger_reference)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
