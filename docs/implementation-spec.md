# 통합 구현 명세 v1.1 (발췌)

> `docs/prd.md` 표에서 "기준 구현 명세"로 지목된 두 문서(통합 구현 명세 v1.1, 엔진 로직 상세 명세 v1.1) 중 이 파일이 실제로 받은 절만 담는다. PRD가 제품 요구사항(무엇을/왜)이라면, 이 문서는 그걸 만족시키는 기술 계약(어떻게)이다 — 번호 체계는 원문 그대로 유지해서 이후 절이 오면 이어 붙인다.
>
> 받은 절: 13(블록체인 기록 상세 명세), 14(보안·개인정보 운영 규칙). 다른 절(1~12, 15~)이 공유되면 이 파일에 같은 방식으로 추가할 것.

---

## 13. 블록체인 기록 상세 명세

블록체인의 역할은 계약 조건이 옳다는 사실을 판단하는 것이 아니라, 특정 시점의 V1·V2 비교 결과와 고객 확인 기록이 이후 변경되지 않았음을 검증하는 것이다. 네트워크와 SDK는 지정하지 않으며, Backend는 **Blockchain Adapter**를 통해 연결한다.

### 13.1 On-chain과 Off-chain 경계

| Off-chain 저장 | On-chain 또는 Ledger 저장 |
| --- | --- |
| 계약서와 설명서 원문 | `recordId` |
| 대출금액·금리·신용정보·고객식별정보 | `payloadHash` |
| Comparison Item 전체 | `schemaVersion` |
| AI Prompt·Output·Evidence Text | `issuer` 또는 `writer` 식별자 |
| Decision 상세와 전자서명 원문 | `recordedAt` 또는 Block Timestamp |
| Canonical Payload 원문 | `supersedesRecordId` (선택) |

### 13.2 Canonical Proof Payload

```json
{
  "schemaVersion": "1.0",
  "proofId": "proof_01",
  "checkIdHash": "sha256:...",
  "applicationIdHash": "sha256:...",
  "v1SnapshotHash": "sha256:...",
  "v2SnapshotHash": "sha256:...",
  "comparisonResultHash": "sha256:...",
  "decisionHash": "sha256:...",
  "issuerId": "im-bank-demo",
  "promptVersion": "extract-v1|explain-v1",
  "ruleVersion": "comparison-rule-v1",
  "calculationVersion": "equal-installment-v1",
  "createdAt": "2026-09-12T08:46:00Z",
  "supersedesRecordId": null
}
```

### 13.3 Hash 생성 순서

1. Snapshot과 Comparison·Decision을 각각 정렬된 Canonical JSON으로 직렬화한다.
2. 문자 인코딩은 UTF-8, 날짜는 UTC ISO-8601, Decimal은 문자열로 고정한다.
3. Canonicalization은 JSON Canonicalization Scheme(RFC 8785)과 동등한 규칙을 사용한다.
4. 각 문서 Hash를 Proof Payload에 넣은 뒤, Proof Payload 전체의 SHA-256 Hash를 계산한다.
5. Ledger에는 Payload 원문이 아니라 `payloadHash`와 `recordId`만 기록한다.
6. `recordId`는 `proofId`를 직접 쓰지 않고, 조직 Salt를 포함한 단방향 Hash로 생성한다.

### 13.4 Blockchain Adapter Contract

```ts
interface BlockchainAdapter {
  anchorRecord(input: {
    recordId: Bytes32,
    payloadHash: Bytes32,
    schemaVersion: string,
    supersedesRecordId?: Bytes32
  }): Promise<{ ledgerReference: string, submittedAt: DateTime }>

  getRecord(recordId: Bytes32): Promise<{
    payloadHash: Bytes32,
    schemaVersion: string,
    issuer: string,
    recordedAt: DateTime,
    supersedesRecordId?: Bytes32
  } | null>

  getConfirmation(ledgerReference: string): Promise<{
    status: PENDING | CONFIRMED | FAILED,
    confirmationCount: number
  }>
}
```

### 13.5 Smart Contract 또는 Ledger 규칙

- 동일 `recordId`는 한 번만 등록할 수 있다. 중복 등록은 기존 `payloadHash`가 같을 때 Idempotent Success로 처리한다.
- 기존 Record를 Update·Delete하지 않는다. 수정이 필요하면 새 `recordId`를 만들고 `supersedesRecordId`를 연결한다.
- 쓰기 권한은 승인된 Issuer만 가진다. 고객은 읽기 검증만 수행한다.
- Record Created Event에는 `recordId`·`payloadHash`·`schemaVersion`·`issuer`·`recordedAt`을 포함한다.
- 원문 복원이 가능한 값, 고객 ID, 계좌번호, 금액, 금리, 문서 URL은 Event에도 넣지 않는다.

### 13.6 제출과 재시도

| 시점 | AnchorStatus | 처리 |
| --- | --- | --- |
| Proof 생성 | `PENDING` | Outbox Event 생성 |
| Ledger 제출 성공 | `SUBMITTED` | `ledgerReference` 저장 |
| 설정 Confirmation 충족 | `CONFIRMED` | `verified = true` |
| Timeout 또는 일시 오류 | `PENDING` | 1분·5분·15분·60분 간격 재시도 |
| 최대 8회 실패 | `FAILED` | 운영 Alert, 고객 화면은 "기록 확인 지연" |

### 13.7 검증 API 로직

```
verify(proofId):
  payload = loadOffChainCanonicalPayload(proofId)
  recomputedHash = SHA256(canonicalize(payload))
  record = blockchainAdapter.getRecord(hashWithSalt(proofId))
  if record is null: return {verified:false, reason:"NOT_ANCHORED"}
  if record.payloadHash != recomputedHash: return {verified:false, reason:"HASH_MISMATCH"}
  return {verified:true, recordedAt:record.recordedAt, ledgerReference:...}
```

---

## 14. 보안·개인정보 운영 규칙

| 영역 | 필수 규칙 |
| --- | --- |
| 인증 | 모든 고객 API는 기존 은행 인증 Session을 사용하고 `applicationId` 소유권을 확인 |
| 권한 | 고객·상담원·운영자·AI Worker·Ledger Writer 역할을 분리 |
| 전송 | 모든 API와 Object Storage 접근은 암호화 통신 |
| 저장 | 원문과 추출 Text는 암호화 저장. Hash와 식별자는 별도 저장 |
| AI 입력 | PII Masking 후 최소 Block만 전달 |
| Log | 문서 원문·Source Text·고객 금융값을 Application Log에 기록하지 않음 |
| 원문 URL | 인증 후 짧은 만료시간의 Signed URL 발급, 재사용 차단 |
| Secret | AI Key·Ledger Signer Key·Salt를 코드와 일반 환경파일에 저장하지 않음 |
| 삭제 | Demo 데이터는 행사 종료 후 30일 이내 삭제. 운영 보존기간은 법무·준법 검토 후 결정 |
| 감사 | Snapshot 생성·Comparison 실행·Decision·Anchor·Verify 이벤트를 Append-only Audit Log에 저장 |

---

## 이 서버(IM-SERVER)에 반영한 내용

이 절을 받은 뒤 아래 문서를 함께 갱신했다 — 기존 설계(13장 이전에 만든 초안)와 충돌하는 부분은 이 명세를 기준으로 교체했다.

- `docs/db-design.md` 3.2 `comparison_runs`: 단일 `policy_version` 컬럼을 `prompt_version`/`rule_version`/`calculation_version` 3개로 분리(13.2 Payload 필드와 1:1).
- `docs/db-design.md` 3.4 `proof_records`/`proof_verifications`: Canonical Payload 필드(`check_id_hash`, `application_id_hash`, `issuer_id` 등), `AnchorStatus`를 `PENDING → SUBMITTED → CONFIRMED / FAILED`로, `ledger_reference`·`confirmation_count`·재시도 스케줄 컬럼을 반영해 재작성.
- `src/main/resources/db/migration/V5__align_proof_tables_with_blockchain_adapter_contract.sql`: 위 변경을 실제 스키마에 반영.
- `docs/api-design.md` 6장 Proof 리소스: `GET /api/proof/{proofId}` 응답 필드와 `POST /api/proof/{proofId}:verify` 응답을 13.7 `verify()` 로직(`NOT_ANCHORED`/`HASH_MISMATCH`)에 맞춰 갱신.
- `BlockchainAdapter` 인터페이스(13.4)는 실제 구현 시 `proof` 모듈의 `api/BlockchainAdapter.java`에 위치시킬 것 — 모듈 밖에서는 이 인터페이스로만 접근하고, 특정 네트워크/SDK 구현체는 `proof/internal/`에 둔다(CLAUDE.md 모듈 경계 원칙 그대로 적용).
- 14장 보안 규칙 중 "역할 분리"(고객/상담원/운영자/AI Worker/Ledger Writer)는 아직 인증·인가 설계가 없어 미반영 — 실제 인증 모듈을 만들 때 반드시 적용할 것(7장 미결 사항에 추가).
