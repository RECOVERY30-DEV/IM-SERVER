# IM 조건체크 데이터베이스 설계서 (MySQL)

> 기준: `docs/prd.md` (PRD v1.0) + `docs/wireframes.md`의 S01~S06, E01 화면(Figma `공모전팀` 파일) + `docs/implementation-spec.md` 13장(블록체인 기록 상세 명세)
> DBMS: **MySQL 8.0** (InnoDB, `utf8mb4`)
> 마이그레이션: Flyway (`src/main/resources/db/migration`), `spring.jpa.hibernate.ddl-auto=validate`
> 버전: v1.1 — `proof_*`를 Blockchain Adapter Contract(13장)에 맞춰 재설계 (`V5` 마이그레이션)

이 서비스는 대출 심사·승인·상품추천을 직접 수행하지 않는다(PRD 5.2 Non-goals). 따라서 고객·신청건·상품·계약서 자체는 **외부 코어뱅킹/전자약정 시스템이 원본을 소유**하고, 이 DB는 그 시스템이 내려준 값의 **불변 Snapshot과 비교·결정·증빙 기록**만 가진다. `application_id`, `customer_id`, `contract_document_id`는 모두 외부 시스템의 opaque 식별자로 취급한다.

---

## 0. 설계 원칙 (PRD 안전장치의 DB 반영)

| PRD 원칙 | DB 반영 |
| --- | --- |
| V1·V2는 수정 불가능한 Snapshot (FR01, FR02) | `condition_pre_snapshots` / `condition_post_snapshots`에 UPDATE 컬럼 없음(생성 컬럼만), 앱 레벨에서도 UPDATE 금지 — 재조회·재심사는 새 row |
| 불확실 항목을 "변경 없음"으로 표시하는 False Safe 금지 (5.1, 20장) | `comparison_items.item_status`에 `UNKNOWN`을 1급 값으로 두고, `comparison_runs.overall_status` 계산은 애플리케이션이 아니라 이 값들의 집계 규칙(4장)을 그대로 SQL/도메인 로직에 반영 |
| 사유·근거 없이 AI가 원인을 만들지 않음 (FR10) | `comparison_item_evidence.reason_code/reason_text`는 NULL 허용 — 값이 없으면 화면에 "상담으로 확인" 문구, DB가 빈 값을 강제로 채우지 않음 |
| 필수 변경 미확인 시 진행 차단 (FR11) | `decision_required_reviews`에 항목별 `reviewed_at`을 별도 저장 — PROCEED 여부는 이 테이블 전체가 채워졌는지로 판정(앱 레벨 게이트) |
| Decision 저장 전 계약 API 호출 금지, Ledger 장애가 계약완료를 막지 않음 (FR12, FR14) | `decisions`(계약 진행 결정)와 `proof_records`(원장 anchoring)를 별도 트랜잭션/테이블로 분리 — anchor 실패가 decisions 행에 영향 없음 |
| On-chain에는 Hash·Version만, PII·원문·금융값 금지 (12.4) | `proof_records`에는 각 단계 Hash와 `policy_version`만 저장, 원문·금액은 `condition_*`/`comparison_*` 쪽 Off-chain 테이블에만 존재 |
| Snapshot 생성·비교·Review·Decision·Anchor·Verify는 Append-only Audit Event (15장) | `audit_condition_events` 단일 append-only 로그, UPDATE/DELETE 없음 |

---

## 1. 모듈(bounded context) ↔ 테이블 접두사

| 모듈 패키지(예정) | 테이블 접두사 | 책임 (PRD 매핑) |
| --- | --- | --- |
| `condition` | `condition_` | V1/V2 Snapshot 저장 (FR01~FR03) |
| `comparison` | `comparison_` | 추출·정규화·비교·계산·근거 (FR04~FR10) |
| `decision` | `decision_` | Review Gate + 진행/재검토/상담 결정 + 전자서명 (FR11, FR12) |
| `proof` | `proof_` | Hash anchoring과 무결성 검증 (FR13, FR14) |
| `consultation`(경량, referral만) | `consultation_` | "상담원에게 문의" 핸드오프 기록. 실제 상담·채팅은 Non-goal(ProofTalk 통합 제외) — 여기선 참조만 남김 |
| 공통 | `audit_` | Append-only 감사 이벤트 |

> 모듈 경계는 recovery-server와 동일하게 ArchUnit으로 강제한다(각 모듈 `internal`/`domain` 외부 참조 금지). 이 문서는 데이터 계층까지만 다루고, `api/`·슬라이스 핸들러 설계는 별도 작업이다.

---

## 2. 마이그레이션 이력

| 파일 | 내용 |
| --- | --- |
| `V1__create_condition_tables.sql` | `condition_pre_snapshots`, `condition_post_snapshots` |
| `V2__create_comparison_tables.sql` | `comparison_runs`, `comparison_run_steps`, `comparison_items`, `comparison_item_evidence`, `comparison_impacts` |
| `V3__create_decision_and_proof_tables.sql` | `decision_required_reviews`, `decisions`, `proof_records`, `proof_verifications` |
| `V4__create_audit_and_consultation_tables.sql` | `audit_condition_events`, `consultation_referrals` |
| `V5__align_proof_tables_with_blockchain_adapter_contract.sql` | `comparison_runs`의 `policy_version` → `prompt_version`/`rule_version`/`calculation_version` 분리, `proof_records`/`proof_verifications`를 13장(Blockchain Adapter Contract) 기준으로 재작성 |

실제 SQL은 `src/main/resources/db/migration/`에 위 파일명 그대로 있다 (엔티티는 아직 없음 — Hibernate `validate` 모드는 매핑 안 된 테이블 존재 자체는 검증하지 않으므로 먼저 스키마만 있어도 기동에 문제없다). 도메인 모듈을 실제로 만들 때 엔티티를 이 스키마에 맞춰 작성할 것.

---

## 3. 테이블 상세

### 3.1 `condition_` — V1/V2 Snapshot

**`condition_pre_snapshots`** (V1, S01 "이 조건으로 신청")
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `application_id` | 외부 대출 신청 ID |
| `customer_id` | 외부 고객 ID |
| `product_code` | 상품코드 (개인 신용대출 1종) |
| `product_version` | 사전조회 시점 상품·우대조건 정책 Version |
| `idempotency_key` | **UNIQUE** — FR01 "동일 키 재호출 시 같은 Snapshot 반환" |
| `snapshot_payload` | JSON — PRD 10장 Field Code(LOAN_AMOUNT, BASE_RATE, SPREAD_RATE, PREFERENTIAL_RATE, FINAL_RATE, TERM_MONTHS, REPAYMENT_METHOD, PREFERENTIAL_CONDITIONS[항목명+%p], STAMP_TAX, FIXED_UPFRONT_FEES, DELINQUENCY_RATE) → 정규화된 값 |
| `payload_hash` | SHA-256(canonical JSON) — Proof에서 재계산 대조 |
| `inquired_at` | 사전조회 시각 |
| `expires_at` | 유효기간 (S01 "오늘 18:00까지", E01의 판정 기준) |
| `created_at` | |

불변: 앱에서 UPDATE 금지, 재조회는 새 row. `expires_at` 지난 뒤 신청 확정 시도는 E01로 분기(비교 기준 생성 자체를 차단).

**`condition_post_snapshots`** (V2, 최종 심사 완료 시)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `application_id` | |
| `pre_snapshot_id` | FK → `condition_pre_snapshots.id` — 신청 당시 유효했던 V1 (18.2 "HISTORICAL_VALID" 판정 기준) |
| `review_version` | 최종 심사 Version |
| `contract_document_id` | 전자약정서 식별자(외부 문서 시스템) |
| `contract_document_hash` | 계약서 원문 Hash — FR03 Version Integrity(S05 재검증, 서명 직전 Hash 변경 시 진행 차단) |
| `snapshot_payload` | JSON — V2 최종조건 값(V1과 동일 Field Schema) |
| `payload_hash` | |
| `decided_at` | 최종 심사 완료 시각 |
| `created_at` | |

불변: 계약서가 바뀌면 새 row(새 Version)를 만들고 기존 row는 그대로 둔다. `UNIQUE(contract_document_id, contract_document_hash)`.

### 3.2 `comparison_` — 추출·정규화·비교·계산

**`comparison_runs`** (S02 진행상태, S03/S04 결과의 부모)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `application_id` | |
| `pre_snapshot_id` / `post_snapshot_id` | FK |
| `status` | `PENDING`·`RUNNING`·`COMPLETED`·`FAILED` — Job 상태 |
| `overall_status` | `NO_CHANGE`·`BURDEN_DECREASE`·`CHECK_REQUIRED`·`UNCERTAIN` (14.1 정책 결과, 완료 전 NULL) |
| `uncertain_reason` | `EXTRACTION_FAILED`·`SOURCE_CONFLICT`·`CALCULATION_UNAVAILABLE` — S03 UNCERTAIN 변형 화면("표 위치가 달라 자동 연결 실패" 등)에 노출되는 유형 |
| `total_steps` / `completed_steps` | S02 "6 / 8 항목" |
| `progress_percent` | S02 게이지(파생값, `completed_steps/total_steps*100`을 앱에서 계산해 저장하거나 조회 시 계산) |
| `prompt_version` | AI 추출/설명에 쓴 프롬프트 Version (`implementation-spec.md` 13.2, 예: `extract-v1\|explain-v1`). AI를 안 쓴 필드만 있으면 NULL 가능 |
| `rule_version` | 비교 Rule Engine Version(`comparison-rule-v1`) |
| `calculation_version` | 계산 Engine Version(`equal-installment-v1`) |
| `comparison_hash` | 완료 시 canonical hash |
| `started_at` / `completed_at` | |

`UNIQUE(pre_snapshot_id, post_snapshot_id, rule_version, calculation_version)` — 같은 조합 재계산 방지(재비교가 필요하면 Rule/Calculation Version을 올리거나 새 snapshot을 사용). FR13 재현성("같은 입력·정책 Version이면 동일 Comparison Hash")은 이 세 Version + snapshot 조합으로 성립한다 — 세 Version을 하나로 뭉뚱그리지 않는 이유는 `implementation-spec.md` 13.2 Canonical Payload가 `promptVersion`/`ruleVersion`/`calculationVersion`을 각각 별도 필드로 On-chain에 남기기 때문(AI를 안 쓴 재계산과 AI 프롬프트만 바뀐 재추출을 구분해서 감사할 수 있어야 함).

**`comparison_run_steps`** (S02 4개 체크리스트 — 내부 처리 단계와 UI 표시 그룹을 분리)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK |
| `step_code` | 내부 처리 단계 (FR04~FR09 세부 단위, PRD 13장 Rule·Calculation 역할과 1:1) |
| `step_order` | |
| `display_group` | S02/E02 화면에 보이는 4개 그룹 중 하나 (예: `AMOUNT_RATE_LINKED`, `PREFERENTIAL_CHECKED`, `TERM_METHOD_CHECKED`, `PAYMENT_INTEREST_CALCULATED`) — `docs/wireframes.md`에 남긴 "내부 8단계 ↔ 화면 4단계 매핑 필요" 메모의 실제 저장소 |
| `status` | `PENDING`·`DONE`·`FAILED` |
| `completed_at` | |

`UNIQUE(comparison_run_id, step_code)`.

**`comparison_items`** (FR07, S03/S04 핵심 — Field별 비교 결과)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK |
| `field_code` | PRD 10장 Field Code (`LOAN_AMOUNT`, `FINAL_RATE`, `TERM_MONTHS`, `PREPAYMENT_FEE`, `MONTHLY_PAYMENT` 등) |
| `contributes_to_overall` | `MONTHLY_PAYMENT`·`TOTAL_INTEREST`는 FALSE (10장: "계산된 Impact, 계약변경 건수에는 미포함") — 14.2 필수확인 판정에 사용 |
| `item_status` | `SAME`·`BETTER`·`WORSE`·`STRUCTURAL_CHANGE`·`UNKNOWN` |
| `v1_value_text` / `v2_value_text` | 화면 표시용 정규화 문자열 ("연 5.20%") |
| `v1_value_numeric` / `v2_value_numeric` | 계산·정렬용 수치 |
| `unit` | `PERCENT`·`WON`·`MONTHS`·`ENUM` 등 |
| `delta_numeric` | S03 "+0.30%p", "+18,400원" |
| `source` | `STRUCTURED_API`·`DOCUMENT_AI`·`MANUAL` — FR04/05 Source Resolution 결과 |
| `confidence` | AI 추출 신뢰도(Confidence Gate 판단 근거) |
| `unknown_reason` | S03 UNCERTAIN 변형의 "표 위치가 달라 자동 연결 실패" 같은 사유 텍스트 |
| `requires_review` | `WORSE`·`STRUCTURAL_CHANGE`·`UNKNOWN` 이면서 `contributes_to_overall`인 항목의 캐시 플래그(14.2) |

`UNIQUE(comparison_run_id, field_code)`.

**`comparison_item_evidence`** (FR10, S04 "확인된 변경 사유" + "서명 [필수] 최종 약정서 3쪽")
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_item_id` | FK |
| `reason_code` | 없으면 NULL — "사유 없으면 AI 추정 금지" 원칙 그대로 반영 |
| `reason_text` | 고객향 설명 |
| `source_document_id` | 원문 문서 식별자 |
| `source_page` | |
| `source_span_text` | "최종 약정서 3쪽 · 금리 산정 항목" |
| `is_required` | S04 "[필수]" 배지 |

**`comparison_impacts`** (FR08/FR09, S03/S04 "예상 부담 변화" 카드 — 1 run당 1행)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK, `UNIQUE` |
| `v1_monthly_payment` / `v2_monthly_payment` / `monthly_payment_delta` | |
| `v1_total_interest` / `v2_total_interest` / `total_interest_delta` | |
| `fixed_fee_delta` | |
| `total_cost_delta` | 14.3 정책: `total_interest_delta + fixed_fee_delta` (월 납입액·원금 Delta는 미포함) — `CHECK`로 강제 |
| `calculation_basis` | 상환방식(원리금균등 등) |
| `rounding_rule` | 기본 `ROUND_HALF_UP` |
| `calculated_at` | S04 "계산 기준일 2026.09.12" |

### 3.3 `decision_` — Review Gate + 결정 + 전자서명

**`decision_required_reviews`** (FR11, S04 항목별 "확인" 버튼)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK |
| `comparison_item_id` | FK, `UNIQUE` — `requires_review = TRUE`인 항목만 생성 |
| `reviewed_at` | NULL이면 미확인. PROCEED 활성화 조건(FR11 AC)은 "이 테이블의 모든 행이 `reviewed_at NOT NULL`"로 앱이 판정 |

**`decisions`** (FR12, S05 "현재 조건으로 약정하기" / S03 "기존 절차로 계속")
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK, `UNIQUE` — 재비교되면 새 run·새 decision |
| `decision_type` | `PROCEED`·`RECONSIDER`·`CONSULT` |
| `bypassed_uncertain_items` | S03 UNCERTAIN 화면의 "기존 절차로 계속" — 미해결 UNKNOWN 항목이 있는 채로 진행했는지(컴플라이언스상 반드시 남겨야 하는 사실) |
| `all_items_confirmed` | 저장 시점 게이트 스냅샷(감사용) |
| `signature_method` | 기본 `ELECTRONIC` |
| `signed_at` | `PROCEED`면 필수 |
| `signature_valid_until` | S05 "유효시간 04:51" 카운트다운 기준 |
| `post_snapshot_id_at_decision` | 서명 시점에 검증한 V2 — 이후 계약 Version이 또 바뀌면 이 값과 최신 V2를 비교해 재검증 유도(FR03) |
| `contract_document_hash_at_decision` | 서명 시점 계약서 Hash |
| `created_at` | |

`CHECK (decision_type <> 'PROCEED' OR signed_at IS NOT NULL)`.

### 3.4 `proof_` — 무결성 검증

`implementation-spec.md` 13장(Blockchain Adapter Contract)을 그대로 반영한 구조. 컬럼명은 13.2 Canonical Proof Payload의 필드명과 1:1로 맞춰서, Off-chain에 저장된 값으로 언제든 Payload를 재구성 → 재해싱 → 원장 대조(13.7 `verify()`)할 수 있게 한다.

**`proof_records`** (FR13, S06 "조건 확인 기록 · 검증됨")
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK(내부 surrogate) |
| `decision_id` | FK, `UNIQUE` |
| `proof_id` | 13.2의 `proofId` (예: `proof_01`) — Off-chain에서 이 Proof를 가리키는 외부 식별자, `UNIQUE` |
| `record_id_hash` | 13.3.6 "recordId는 proofId를 직접 쓰지 않고 조직 Salt를 포함한 단방향 Hash로 생성" — 실제 원장에 올라가는 `recordId`, `UNIQUE` |
| `schema_version` | 기본 `1.0` |
| `check_id_hash` / `application_id_hash` | `comparison_run_id` / `application_id`를 각각 Salt와 함께 Hash한 값 — 원문 식별자를 On-chain에 노출하지 않기 위함(13.1) |
| `v1_snapshot_hash` / `v2_snapshot_hash` / `comparison_result_hash` / `decision_hash` | 각 단계 원문의 canonical Hash |
| `issuer_id` | 13.5 "쓰기 권한은 승인된 Issuer만" — 이 Proof를 발급한 Issuer |
| `prompt_version` / `rule_version` / `calculation_version` | 해당 `comparison_runs` 행의 값을 그대로 복사(Payload 재구성용 — run이 나중에 바뀌어도 anchoring 당시 Version이 고정되도록) |
| `payload_hash` | Canonical Payload 전체의 SHA-256 — On-chain에 실제로 기록되는 값 |
| `supersedes_record_id_hash` | 이전 Proof를 대체하는 경우만 채움(13.5 "Update·Delete 대신 새 recordId + supersedesRecordId") |
| `anchor_status` | `PENDING`→`SUBMITTED`→`CONFIRMED`\|`FAILED` (13.6) |
| `ledger_reference` | `anchorRecord()` 응답의 `ledgerReference` |
| `confirmation_count` | `getConfirmation()` 응답 캐시 |
| `submitted_at` / `confirmed_at` | |
| `anchor_retry_count` / `next_retry_at` | 13.6 재시도 스케줄(1·5·15·60분 간격, 최대 8회) 진행 상태 |
| `created_at` | |

**`proof_verifications`** (S06 "기록 자세히 보기", 사후 검증 요청 이력)
| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `proof_record_id` | FK |
| `requested_by` | `CUSTOMER`·`COUNSELOR`·`COMPLIANCE`·`SYSTEM` |
| `recomputed_hash` | 검증 시 Off-chain Payload로 재계산한 Hash |
| `verify_result` | `VERIFIED`·`NOT_ANCHORED`·`HASH_MISMATCH` — 13.7 `verify()`의 반환 사유 그대로 |
| `ledger_recorded_at` | 원장 조회 성공 시 `record.recordedAt` |
| `verified_at` | |

### 3.5 `consultation_referrals` (경량 — 실제 상담은 Non-goal)

S03의 "상담원에게 문의" + 665:7865 "전송 동의" 안내 오버레이("전송 동의가 없어도 예약은 완료됩니다") 대응.

| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `comparison_run_id` | FK |
| `application_id` | |
| `transfer_consent_granted` | 상담원에게 사전 맥락 전달 동의 여부 — 미동의여도 예약 자체는 성립(recovery-server `recovery_consultations.transfer_consent_granted`와 동일 정책) |
| `context_snapshot` | 동의 시에만 채움. PII·원문 제외, reason_code/uncertain 항목 코드 정도만 |
| `external_consultation_ref` | 외부 상담 시스템 예약 ID(연동 시) |
| `requested_at` | |

### 3.6 `audit_condition_events` (append-only)

| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `event_type` | `V1_SAVED`·`V2_SAVED`·`COMPARISON_STARTED`·`COMPARISON_COMPLETED`·`ITEM_REVIEWED`·`DECISION_SUBMITTED`·`PROOF_ANCHORED`·`PROOF_VERIFIED` |
| `application_id` | |
| `actor_type` | `CUSTOMER`·`COUNSELOR`·`SYSTEM` |
| `ref_type` / `ref_id` | 대상 테이블명 + PK |
| `event_payload` | 상태값/코드만(15장: 금융값·원문·PII는 Application Log에 저장 금지) |
| `occurred_at` | |

UPDATE/DELETE 없음(append-only). `INDEX(application_id, occurred_at)`.

---

## 4. 상태 전이 규칙

### `comparison_runs.overall_status` (PRD 14.1 그대로)
```
if any required item is UNKNOWN:
  UNCERTAIN
else if any item is WORSE or STRUCTURAL_CHANGE:
  CHECK_REQUIRED
else if any item is BETTER and all other items are SAME or BETTER:
  BURDEN_DECREASE
else if all items are SAME:
  NO_CHANGE
else:
  UNCERTAIN
```
`required item` = `comparison_items.contributes_to_overall = TRUE`인 행.

### `decision_required_reviews` 생성 규칙 (PRD 14.2)
```
comparison_items.contributes_to_overall = TRUE
  AND comparison_items.item_status IN ('WORSE','STRUCTURAL_CHANGE','UNKNOWN')
  → decision_required_reviews 1행 생성 (reviewed_at = NULL)
```

### `condition_pre_snapshots` 유효성 (E01, 18.1/18.2)
```
신청 확정 시각 > expires_at            → 비교 기준 생성 자체를 차단 (E01)
신청 당시엔 유효했으나 비교 시점엔 만료  → HISTORICAL_VALID로 비교 진행 (condition_post_snapshots.pre_snapshot_id로 이미 고정돼 있으므로 자동 성립)
```

### `comparison_runs.status`
```
PENDING → RUNNING → COMPLETED (overall_status 확정)
                  → FAILED (uncertain_reason 기록, S02/E02로 분기)
```

### `decisions` / `proof_records` (FR12, FR14)
```
decision_required_reviews 전체 reviewed_at NOT NULL 확인
  → decisions 저장 (signed_at 기록) — 이 저장이 성공해야만 외부 계약 체결 API 호출
  → proof_records 생성 (anchor_status = PENDING)
       → 원장 성공: ANCHORED
       → 원장 실패: FAILED, anchor_retry_count 증가, 비동기 재시도 (계약 완료 자체는 영향 없음)
```

---

## 5. 리소스별 API 매핑

상세 스펙(요청/응답 필드, 에러코드, 화면 조합)은 [`docs/api-design.md`](./api-design.md)가 정본이다. 여기서는 리소스와 테이블의 대응만 요약한다.

| 모듈 | 리소스 | 테이블 |
| --- | --- | --- |
| `condition` | pre-conditions, post-conditions | `condition_pre_snapshots`, `condition_post_snapshots` |
| `comparison` | comparisons, comparisons/items | `comparison_runs`, `comparison_run_steps`, `comparison_items`, `comparison_item_evidence`, `comparison_impacts` |
| `decision` | review-gate, signature-sessions, decisions | `decision_required_reviews`, `decisions` |
| `proof` | proof | `proof_records`, `proof_verifications` |
| `consultation` | consultation-referrals | `consultation_referrals` |

---

## 6. Mock data 사용 영역 (MVP)

| 영역 | 처리 |
| --- | --- |
| `condition_pre_snapshots` / `condition_post_snapshots` | **시드**. 외부 코어뱅킹 연동 전이므로 PRD 예시값(대출한도 5,000만원, 연 5.20%→5.50% 등) 그대로 목 데이터로 고정 |
| AI 추출(`comparison_items.source = DOCUMENT_AI`, `confidence`) | **목 값**. 실제 문서 파싱 붙이기 전까지 사전 정의된 후보 세트 사용 |
| `comparison_impacts` 계산 로직 | **목 아님** — 원리금균등 계산은 결정론적 로직이라 실제 구현 필요 (PRD FR08 AC: 마지막 회차 잔액 0원) |
| `proof_records.anchor_*` | **목 원장**. 실제 블록체인 대신 로컬 append-only 해시체인이나 목 anchoring 서비스로 대체 가능(PRD 12.1: 은행 단독 MVP 기준 필요도 6/10) |
| `consultation_referrals.external_consultation_ref` | **목 값**. 실제 상담 시스템 연동은 Non-goal |

### 목 아님 — 실제 로직 필요
`comparison_runs.overall_status` 계산(4장 규칙) · `comparison_items` 비교 판정 · `comparison_impacts` 원리금균등 계산 · `decision_required_reviews` 게이트 판정 · `decisions` 서명 유효시간 만료 처리 · `proof_records` Hash 재계산/대조

---

## 6.5 구현 우선순위

| 순위 | 테이블 | 사유 |
| --- | --- | --- |
| **P0** | `condition_pre_snapshots`, `condition_post_snapshots` | V1/V2 없이는 아무 것도 시작 못함 |
| **P0** | `comparison_runs`, `comparison_items`, `comparison_impacts` | S03 변경요약 렌더의 최소 집합 |
| **P0** | `decision_required_reviews`, `decisions` | S05 서명 전 게이트 — Release Acceptance Criteria 핵심 항목 다수가 여기 걸림 |
| **P1** | `comparison_run_steps` | S02 진행상태 화면(체감 UX, 없어도 최종 결과엔 영향 없음) |
| **P1** | `comparison_item_evidence` | S04 근거/사유 — 화면상 중요하지만 S03까지는 없어도 시연 가능 |
| **P1** | `proof_records`, `proof_verifications` | S06 검증기록 — 블록체인 자체가 6/10 필요도(12.1)라 후순위 가능 |
| **P2** | `consultation_referrals`, `audit_condition_events` | 감사·핸드오프는 심사 어필용, 핵심 데모 경로엔 없어도 됨 |

---

## 7. 확인이 필요한 미결 사항

1. **외부 코어뱅킹/전자약정 시스템 연동 방식** — REST 콜백인지, 이벤트 스트림인지에 따라 `condition_post_snapshots` 생성 트리거 방식이 달라짐.
2. **내부 8단계 ↔ S02 화면 4단계 매핑 확정** — `comparison_run_steps.display_group` 값 목록을 실제 FR04~FR09 세부 구현과 맞춰 확정해야 함(`docs/wireframes.md` S02 절 참고).
3. **전자서명 원본 보관 위치** — PRD 12.4는 "Decision·전자서명 원문은 Off-chain"이라고만 명시. 서명 이미지 자체를 이 DB에 저장할지, 외부 전자서명 시스템 참조 ID만 가질지 미정 (`decisions` 테이블에 `signature_ref` 컬럼 추가 여지).
4. **재비교 허용 범위** — 같은 `application_id`에 대해 `condition_post_snapshots`가 여러 번 생성될 수 있는지(재산정), 그때마다 `comparison_runs`를 몇 개까지 유지할지 보관 정책.
5. **`comparison_items.v1_value_numeric`/`v2_value_numeric`의 단위 통일** — 금액(원)과 비율(%)이 같은 컬럼을 공유하는 설계라 `unit` 값에 따른 애플리케이션 레벨 검증이 필요.
6. **Proof anchor 대상(원장) 선정 전까지 `ledger_reference` 포맷 미정** — 네트워크·SDK는 `implementation-spec.md` 13장이 의도적으로 지정하지 않았음, `BlockchainAdapter` 구현체 선정 후 확정.
7. **역할 분리(고객/상담원/운영자/AI Worker/Ledger Writer) 미반영** — `implementation-spec.md` 14장이 요구하는 권한 분리를 적용할 인증·인가 모듈이 아직 없음. 첫 인증 모듈을 만들 때 이 역할 구분을 반드시 반영할 것.
