-- repaymentMethod 값(예: EQUAL_PRINCIPAL_INTEREST, 25자)이 그대로 들어가는데
-- 기존 VARCHAR(20)은 짧아서 Data truncation 500 에러가 났다. 엔티티 필드 길이 상향과 짝을 맞춘다.
ALTER TABLE comparison_impacts MODIFY COLUMN calculation_basis VARCHAR(50) NOT NULL;
