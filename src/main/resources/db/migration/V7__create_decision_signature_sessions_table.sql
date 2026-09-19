-- S05 "전자서명" 패드의 유효시간 카운트다운(FR12) 저장소.
CREATE TABLE decision_signature_sessions (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    comparison_run_id   BIGINT NOT NULL,
    session_id          VARCHAR(64) NOT NULL,
    valid_until         DATETIME(6) NOT NULL,
    used_at             DATETIME(6) NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_signature_sessions_session_id (session_id),
    KEY idx_decision_signature_sessions_run (comparison_run_id),
    CONSTRAINT fk_decision_signature_sessions_run
        FOREIGN KEY (comparison_run_id) REFERENCES comparison_runs (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
