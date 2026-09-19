# IM 조건체크 API 설계서

> 기준: `docs/prd.md`(PRD v1.0), `docs/wireframes.md`(S01~S06·E01 화면), `docs/db-design.md`(테이블 설계)
> 원칙: **화면 단위가 아니라 리소스 단위로 엔드포인트를 나눈다.** 클라이언트가 여러 리소스를 조합해 화면을 구성한다 (recovery-server `docs/db-design.md` 5장과 동일한 팀 컨벤션).
> 아직 컨트롤러(Handler)는 구현되지 않았다 — 이 문서가 슬라이스 구현 시의 Command/Query/Response 스펙이다.

---

## 0. 공통 규칙

- Base path: `/api`
- 모든 응답은 `ApiResponse<T>`로 감싼다: 성공 `{ "success": true, "data": T, "error": null }`, 실패 `{ "success": false, "data": null, "error": { "code", "message" } }` (`shared/response`)
- 인증: 기존 은행 앱 인증 세션을 그대로 사용하고, 매 요청마다 **호출자가 해당 `applicationId`의 소유자인지**(customerId 일치) 검증한다 (PRD 15장) — 아래 스펙에서는 반복 서술하지 않음
- `POST /pre-conditions`, `POST /post-conditions`는 이 서비스가 조건을 산정하지 않고 **호스트 시스템(사전조회 엔진, 심사 엔진)이 계산해 넘겨주는 값을 스냅샷으로 봉인**하는 API다. 즉 클라이언트가 직접 호출하지 않고, 호스트 백엔드가 서버 간 호출로 밀어넣는 것을 기본 가정으로 한다 (표에 "호출자" 명시)
- 시각은 모두 ISO-8601(`Instant`, UTC), 화면 표시는 프론트에서 `Asia/Seoul` 변환
- 금액은 정수 원 단위, 비율은 `DECIMAL` 문자열이 아니라 숫자(예: `5.20`)로 내려주고 `%` 단위임을 필드명(`ratePercent`)으로 명시
- 에러코드는 CLAUDE.md 컨벤션(`{모듈}_{HTTP상태}_{순번}`)을 따른다. 신규 코드는 11장 참고

---

## 1. 리소스: Pre-Condition (사전조건 V1) — 모듈 `condition`

**화면**: S01(사전조건 저장), E01(사전조건 유효기간 만료)

### `POST /api/applications/{applicationId}/pre-conditions`
사전조회가 끝나고 고객이 "이 조건으로 신청"을 누른 시점(또는 조회 즉시)에 V1을 봉인한다.

| 구분 | 내용 |
| --- | --- |
| 호출자 | 호스트 사전조회 시스템 (서버 간) |
| 헤더 | `Idempotency-Key` **필수** — 동일 키 재호출 시 새로 만들지 않고 기존 Snapshot을 그대로 반환 (FR01 AC) |
| Request | `customerId`, `productCode`, `productVersion`, `inquiredAt`, `expiresAt`, `conditions: { loanAmount, baseRatePercent, spreadRatePercent, preferentialRatePercent, finalRatePercent, termMonths, repaymentMethod, preferentialConditions: [{ code, label, ratePercentOff }], stampTax, fixedUpfrontFees, delinquencyRatePercent }` |
| Response | `preSnapshotId`, `applicationId`, `expiresAt`, `payloadHash`, `conditions`(echo) |
| 실패 | `CONDITION_400_1`(필수 Field 누락/단위 불일치), `CONDITION_409_1`(같은 Idempotency-Key인데 payload가 다름 — 재사용 금지 위반) |

### `GET /api/applications/{applicationId}/pre-conditions/latest`
S01 화면 렌더 + E01 판정에 공용으로 쓰는 조회 API.

| 구분 | 내용 |
| --- | --- |
| 호출자 | 모바일 클라이언트 |
| Response | `preSnapshotId`, `status`(`VALID`\|`EXPIRED`), `inquiredAt`, `expiresAt`, `conditions`(위와 동일 구조) |
| 화면 매핑 | `status=VALID` → S01 카드 그대로 렌더. `status=EXPIRED` → E01로 라우팅(같은 응답의 `inquiredAt`/`expiresAt`을 E01 "현재상태/조회일시/유효기간" 행에 그대로 사용) |
| 실패 | `CONDITION_404_1`(조회 이력 자체가 없음) |

### `GET /api/applications/{applicationId}/pre-conditions?limit=`
E01의 "이전 조회 결과 보기" — 과거 V1 이력.

| Response | `items: [{ preSnapshotId, inquiredAt, expiresAt, finalRatePercent, loanAmount }]` (요약만, 상세는 위 `latest`와 같은 형태로 `GET /pre-conditions/{id}` 추가 가능) |

---

## 2. 리소스: Post-Condition (최종조건 V2) — 모듈 `condition`

**화면**: 사용자 화면 없음 (내부 트리거) — 생성 즉시 `comparisons` 리소스를 자동 기동시킨다.

### `POST /api/applications/{applicationId}/post-conditions`

| 구분 | 내용 |
| --- | --- |
| 호출자 | 호스트 심사(전자약정) 시스템 (서버 간) — 최종 심사 완료 이벤트 |
| Request | `reviewVersion`, `contractDocumentId`, `contractDocumentHash`, `decidedAt`, `conditions`(V1과 동일 Field Schema) |
| Response | `postSnapshotId`, `preSnapshotId`(자동 매칭된 신청 시점 유효 V1 — `HISTORICAL_VALID` 판정 포함), `comparisonId`(자동 생성된 비교 Job id) |
| 실패 | `CONDITION_404_2`(매칭되는 V1 자체가 없음 — 신청 프로세스 오류), `CONDITION_409_2`(동일 `contractDocumentId`+`Hash` 중복 등록) |

---

## 3. 리소스: Comparison Run (비교 Job) — 모듈 `comparison`

**화면**: S02(비교 진행 중), S03(변경 요약 · 확인필요 / UNCERTAIN 변형)

### `GET /api/comparisons/{comparisonId}`
Job의 현재 스냅샷. S02와 S03이 공용으로 폴링/최초 로드에 쓴다.

| 필드 | 설명 |
| --- | --- |
| `status` | `PENDING`\|`RUNNING`\|`COMPLETED`\|`FAILED` |
| `overallStatus` | `NO_CHANGE`\|`BURDEN_DECREASE`\|`CHECK_REQUIRED`\|`UNCERTAIN` (완료 전 `null`) |
| `uncertainReason` | `EXTRACTION_FAILED`\|`SOURCE_CONFLICT`\|`CALCULATION_UNAVAILABLE` (해당 시) |
| `progress` | `{ percent, completedSteps, totalSteps }` — S02 "75%", "6/8 항목" |
| `steps` | `[{ displayGroup, label, status }]` — S02 4개 체크리스트 (`AMOUNT_RATE_LINKED` 등) |
| 실패 | `COMPARISON_404_1`(존재하지 않는 run) |

폴링 주기는 클라이언트 재량이지만 "자동 전환"(PRD 8장)이 요구사항이므로, 후속 구현에서 SSE(`GET /api/comparisons/{comparisonId}/events`)로 승격 여지를 열어둔다 (지금은 폴링 스펙만 확정).

### `GET /api/comparisons/{comparisonId}/summary`
S03 상단 카드 전용 — Overall Status + 비용 영향 + 변경 항목 개수.

| 필드 | 설명 |
| --- | --- |
| `overallStatus` | 위와 동일 |
| `impact` | `{ monthlyPaymentDelta, totalInterestDelta, totalCostDelta }` — S03 "예상 부담 변화: 월 +18,400원, 대출기간 총 +662,400원" |
| `changedItemsCount` / `unchangedItemsCount` | S03 "변경 없는 조건 5개" |
| `requiredReviewCount` / `reviewedCount` | S03 UNCERTAIN 변형 "확인 완료된 조건 6개", "6 / 8 확인" |
| `headlineItems` | `[{ itemId, fieldCode, label, deltaLabel, itemStatus }]` — S03 카드에 바로 나열되는 대표 변경 항목(대출금리, 월 납입액 등) 2~3개 |
| 실패 | `COMPARISON_404_1`, `COMPARISON_409_1`(아직 `status != COMPLETED`인데 호출 — S02에서 잘못된 화면 전환) |

### `POST /api/applications/{applicationId}/comparisons:retry`
UNCERTAIN 상태에서 재추출을 다시 시도하거나, 계약서 Version이 바뀌어 재비교가 필요할 때(FR03) 새 run을 만든다.

| Response | `comparisonId`(새 run) |
| 실패 | `COMPARISON_409_2`(재비교 사유가 없는데 호출 — 최신 V2가 이미 이 run의 대상과 동일) |

---

## 4. 리소스: Comparison Item (Field별 비교 결과) — 모듈 `comparison`

**화면**: S03(목록/카운트), S04(상세 + 확인)

### `GET /api/comparisons/{comparisonId}/items?status=`
S03 "변경 없는 조건 N개" 펼침, S04 진입 전 목록.

| Query | `status`(콤마 구분: `WORSE,STRUCTURAL_CHANGE,UNKNOWN`\|`SAME`\|생략 시 전체) |
| Response | `items: [{ itemId, fieldCode, label, itemStatus, v1ValueText, v2ValueText, deltaLabel, requiresReview, reviewed }]` |

### `GET /api/comparisons/items/{itemId}`
S04 상세 화면을 한 번에 그리는 API.

| 필드 | 설명 |
| --- | --- |
| `fieldCode`, `label` | "대출금리" |
| `v1ValueText`, `v2ValueText` | "연 5.20%", "연 5.50%" |
| `itemStatus`, `deltaLabel` | `WORSE`, "+0.30%p" |
| `reason` | `{ code, text }` \| `null` — "최종 심사 기준으로 급여이체 실적이 확인되지 않아 -0.30%p 우대금리가 반영되지 않았습니다." (없으면 `null` → 화면은 "상담으로 확인해 주세요") |
| `evidence` | `{ documentId, page, spanText, isRequired }` \| `null` — "[필수] 최종 약정서 3쪽 · 금리 산정 항목" |
| `relatedImpacts` | `[{ type: 'MONTHLY_PAYMENT'\|'TOTAL_INTEREST', deltaLabel }]` — S04 "[변경] 월 납입액 +18,400원" 리스트 (클릭 시 `comparison_impacts` 상세로 이동) |
| `calculationBasis` | `{ method: '원리금균등', termMonths, rateDeltaPercent, resultLabel, calculatedAt, roundingRule }` — "원리금균등·36개월, 금리 0.30%p 상승 → 월 +18,400원, 계산기준일 2026.09.12·원 단위 반올림" |
| `unknownReason` | UNKNOWN 항목일 때만 — "표 위치가 달라 자동 연결 실패" |
| `reviewed` / `reviewedAt` | 확인 여부 |
| 실패 | `COMPARISON_404_2`(존재하지 않는 item) |

### `POST /api/comparisons/items/{itemId}:review`
S04 "확인" 버튼 — 필수 확인 처리(FR11).

| Response | `itemId`, `reviewedAt` |
| 부수효과 | 이 item이 마지막 필수 항목이었다면 `review-gate.allReviewed = true`가 됨 |
| 실패 | `COMPARISON_400_1`(확인이 필요 없는 항목 — `SAME`/`BETTER`에 대해 호출), `COMPARISON_409_3`(이미 확인 완료된 항목 재호출은 idempotent하게 200 처리 — 에러 아님) |

---

## 5. 리소스: Review Gate & Decision — 모듈 `decision`

**화면**: S05(약정 결정과 전자서명), S03(UNCERTAIN 변형의 "기존 절차로 계속")

### `GET /api/comparisons/{comparisonId}/review-gate`
S05 진입 시 체크박스·서명 활성화 여부 판단.

| 필드 | 설명 |
| --- | --- |
| `requiredItems` | `[{ itemId, label, reviewed }]` |
| `allReviewed` | S05 "조건 변경 내용·사유·비용 영향 확인" 체크박스를 활성화할 조건 |
| `snapshot` | `{ loanAmount, finalRatePercent, monthlyPayment, termMonths, repaymentMethod }` — S05 상단 요약 카드 |
| `burdenNotice` | `{ rateDeltaPercent, totalCostDelta }` — "금리 +0.30%p · 총 부담 +662,400원" |

### `POST /api/comparisons/{comparisonId}/signature-sessions`
S05 "전자서명" 패드 진입 시 발급하는 서명 유효시간 세션.

| Response | `sessionId`, `validUntil` — S05 "유효시간 04:51" 카운트다운 기준 |
| 실패 | `DECISION_400_1`(`allReviewed=false`인데 서명 시도) |

### `POST /api/comparisons/{comparisonId}/decisions`
S05 "현재 조건으로 약정하기" / S05 "다시 검토" / S03 "기존 절차로 계속".

| Request | `decisionType`(`PROCEED`\|`RECONSIDER`\|`CONSULT`), `signatureSessionId` (PROCEED일 때만 필수), `bypassedUncertainItems`(UNCERTAIN 상태에서 "기존 절차로 계속" 클릭 시 `true`) |
| Response | `decisionId`, `decisionType`, `signedAt`(해당 시) — 이 응답이 성공해야 클라이언트가 비로소 외부 계약 체결 API를 호출한다(FR12 순서 보장은 클라이언트/BFF 책임) |
| 실패 | `DECISION_400_2`(`allReviewed=false`인데 `PROCEED` 시도), `DECISION_409_1`(서명 세션 만료 — "유효시간" 초과, 재발급 필요), `DECISION_409_2`(그사이 계약서 Version이 바뀜 — `contract_document_hash` 불일치, S05 "약정서가 갱신되어 다시 확인" 안내 → `POST /comparisons:retry` 유도) |

---

## 6. 리소스: Proof — 모듈 `proof`

**화면**: S06(약정 완료와 검증기록)

필드/상태값은 `docs/implementation-spec.md` 13장(Blockchain Adapter Contract)을 그대로 따른다.

### `GET /api/decisions/{decisionId}/proof`
S06 상단 요약 + "검증됨" 배지.

| 필드 | 설명 |
| --- | --- |
| `proofId` | "기록 자세히 보기"(`GET /api/proof/{proofId}`) 이동용 |
| `contractNumber`, `loanAmount`, `finalRatePercent`, `decidedAt` | S06 "약정번호" 등 — `contractNumber`는 실제로는 `contractDocumentId`를 그대로 노출한 값(별도 채번 없음) |
| `anchorStatus` | `PENDING`\|`SUBMITTED`\|`CONFIRMED`\|`FAILED` — 배지는 `CONFIRMED`일 때만 "검증됨" |
| `recordSummary` | `{ changedItemsCount, allReviewed, evidenceLinked }` — `evidenceLinked`는 AI 근거 추출 미구현이라 항상 `false` |

### `GET /api/proof/{proofId}`
"기록 자세히 보기" — Canonical Payload 필드·anchor 상세.

| 필드 | 설명 |
| --- | --- |
| `schemaVersion`, `checkIdHash`, `applicationIdHash`, `v1SnapshotHash`, `v2SnapshotHash`, `comparisonResultHash`, `decisionHash`, `issuerId`, `promptVersion`, `ruleVersion`, `calculationVersion`, `payloadHash` | 13.2 Canonical Proof Payload와 동일 필드 |
| `anchorStatus`, `ledgerReference`, `confirmationCount`, `submittedAt`, `confirmedAt`, `anchorRetryCount` | 13.6 제출·재시도 상태 |

### `POST /api/proof/{proofId}:verify`
13.7 `verify(proofId)` 로직 그대로: Off-chain Payload로 Hash 재계산 → `BlockchainAdapter.getRecord()` 조회 → 대조.

| Response | `verified`(bool), `reason`(`NOT_ANCHORED`\|`HASH_MISMATCH`\|`null`), `recomputedHash`, `ledgerReference`, `recordedAt` |
| 실패 | 이 엔드포인트 자체는 200으로 결과를 반환한다(검증 실패도 정상 응답, `verified:false` + `reason`으로 표현) — `PROOF_500_1`은 `HASH_MISMATCH`가 나왔을 때 운영 Alert를 트리거하기 위한 내부 이벤트 코드이지 HTTP 에러가 아님 |

---

## 7. 리소스: Consultation Referral — 모듈 `consultation` (경량)

**화면**: S03 / S03-UNCERTAIN "상담원에게 문의" + "전송 동의" 안내 오버레이

### `POST /api/comparisons/{comparisonId}/consultation-referrals`

| Request | `transferConsentGranted`(bool) — 오버레이 "전송 동의가 없어도 예약은 완료됩니다" |
| Response | `referralId`, `transferConsentGranted`, `externalConsultationRef`(연동 완료 시) |
| 비고 | 실제 상담 예약/채팅 자체는 Non-goal — 이 API는 핸드오프 기록 + (있다면) 외부 상담 시스템 호출까지만 담당 |

---

## 8. 화면 ↔ API 조합

| 화면 | 호출 조합 |
| --- | --- |
| S01 | `GET pre-conditions/latest` |
| E01 | `GET pre-conditions/latest`(status=EXPIRED) + `GET pre-conditions?limit=`("이전 조회 결과 보기") |
| S02 | `GET comparisons/{id}` 폴링 → `status=COMPLETED` 되면 S03로 자동 전환 |
| S03 (정상) | `GET comparisons/{id}/summary` + `GET items?status=SAME`(count) |
| S03 (UNCERTAIN) | `GET comparisons/{id}/summary` + `GET items?status=UNKNOWN`(사유 포함 카드) |
| S03 → 상담 | `POST consultation-referrals` |
| S04 | `GET items/{itemId}` → 확인 시 `POST items/{itemId}:review` |
| S05 | `GET review-gate` → `POST signature-sessions` → `POST decisions`(PROCEED\|RECONSIDER) |
| S03 → 기존 절차 | `POST decisions`(`CONSULT` 또는 `PROCEED`+`bypassedUncertainItems=true`, 정책 확정 필요 — 10장 참고) |
| S06 | `GET decisions/{id}/proof` (+ "기록 자세히 보기" 시 `GET proof/{id}`, `POST proof/{id}:verify`) |

---

## 9. 에러코드 신규 제안 (`shared/exception/ErrorCode` 추가분)

| 코드 | 상태 | 상황 |
| --- | --- | --- |
| `CONDITION_400_1` | 400 | V1 저장 요청의 Field 누락/단위 불일치 |
| `CONDITION_404_1` | 404 | 유효한 V1 조회 이력 없음 |
| `CONDITION_404_2` | 404 | V2 저장 시 매칭되는 V1 없음 |
| `CONDITION_409_1` | 409 | 동일 Idempotency-Key, 다른 payload |
| `CONDITION_409_2` | 409 | 동일 계약서 식별자+Hash 중복 등록 |
| `COMPARISON_404_1` | 404 | 존재하지 않는 comparison run |
| `COMPARISON_404_2` | 404 | 존재하지 않는 comparison item |
| `COMPARISON_400_1` | 400 | 확인이 필요 없는 항목에 `:review` 호출 |
| `COMPARISON_409_1` | 409 | 완료 전 summary 조회 |
| `COMPARISON_409_2` | 409 | 재비교 사유 없이 `:retry` 호출 |
| `DECISION_400_1` | 400 | `allReviewed=false`인데 서명 세션 발급 또는 `PROCEED` 시도 (실제 구현은 두 상황 모두 이 코드 하나로 통일 — 원인이 같은 규칙 위반이라 굳이 나누지 않음) |
| `DECISION_409_1` | 409 | 서명 세션이 없거나 만료·이미 사용됨 |
| `DECISION_409_2` | 409 | 서명 세션 발급 이후 최종 약정서 Version이 바뀜 — `POST comparisons:retry` 유도 |
| `DECISION_409_3` | 409 | 이미 결정이 저장된 비교 (신규 추가 — 설계 당시엔 없었음) |
| `PROOF_404_1` | 404 | 존재하지 않는 proofId |

---

## 10. 확인이 필요한 미결 사항

1. **S03 "기존 절차로 계속" 버튼의 정확한 의미** — UNCERTAIN 항목을 무시하고 일반 서명 절차로 넘어가는 것인지(그렇다면 `decisions`에 `bypassedUncertainItems=true`로 기록), 아니면 이 조건체크 기능 자체를 건너뛰는 것인지(그렇다면 이 서버가 관여하지 않는 별도 플로우) — 기획 확인 필요. 이 문서는 전자로 가정했다.
2. **폴링 vs SSE** — S02 "자동 전환" 요구사항을 짧은 폴링으로 만족시킬지, `GET .../events` SSE로 갈지는 트래픽/인프라 결정 이후 확정.
3. **서명 세션과 Decision의 관계** — 서명 세션 만료 후 재시도 시 `review-gate`를 다시 조회해야 하는지, 세션만 재발급하면 되는지(체크박스 상태는 서버가 유지하므로 재발급만으로 충분할 가능성이 높음, 구현 시 확정).
4. **`comparisons:retry`의 재사용 범위** — 완전히 새 `comparison_runs` row를 만드는지, 실패한 run을 이어서(step 단위 재시도) 처리할지.
