-- comparison_runs.progress_percent가 SMALLINT로 만들어져 있었는데, JPA 엔티티(ComparisonRun.progressPercent, int)는
-- 기본적으로 INTEGER로 매핑되어 ddl-auto=validate가 실패한다(운영 배포 중 발견). 컬럼을 엔티티 매핑에 맞춘다.
ALTER TABLE comparison_runs
    MODIFY COLUMN progress_percent INT NOT NULL;
