# IM-SERVER

**iM 조건체크** — 사전 안내 대출조건(V1)과 최종 약정조건(V2)을 전자서명 직전에 자동 대조하고, 달라진 조건·금전적 영향·원문 근거를 고객이 확인한 뒤 그 비교 기록의 무결성까지 검증할 수 있게 하는 서비스의 백엔드 서버. [recovery-server](https://github.com/RECOVERY30-DEV) 프로젝트의 아키텍처 컨벤션(모듈러 모놀리스 + 버티컬 슬라이스)을 그대로 이어받아 세팅했다.

제품 배경/문제정의/요구사항은 [`docs/prd.md`](./docs/prd.md) (PRD v1.0, 2026-09-12 기준)가 정본이다. 화면 매핑은 [`docs/wireframes.md`](./docs/wireframes.md), DB 스키마는 [`docs/db-design.md`](./docs/db-design.md), 리소스별 API 스펙은 [`docs/api-design.md`](./docs/api-design.md)가 정본이다.

2026-09-19 기준 `condition`(V1/V2 Snapshot)·`comparison`(비교/계산 엔진)·`decision`(Review Gate·전자서명·서명세션)·`proof`(무결성 증빙, Mock Blockchain Adapter)·`consultation`(상담 핸드오프) 다섯 모듈이 모두 구현되어 신청→최종심사→자동비교→필수확인→서명→약정완료→증빙검증까지 전체 흐름이 동작한다. AI 문서추출(비정형 근거·사유)만 별도 AI 연동이 필요해 빠져 있다 — 새 기능을 추가하기 전에 `CLAUDE.md`와 `docs/prd.md`를 먼저 읽을 것.

## 기술 스택
- Java 25 / Spring Boot 4.1.1
- Gradle
- MySQL 8 + Flyway (스키마 마이그레이션)
- QueryDSL (OpenFeign 포크)
- springdoc-openapi (Swagger UI)
- ArchUnit (모듈 경계 강제 테스트)
- spotless + google-java-format

## 아키텍처
- **모듈 = Bounded Context**, 모듈 간 직접 참조 금지 (동기: `api/` 인터페이스, 비동기: `shared/event/`)
- **모듈 내부 = 버티컬 슬라이스**: Controller/Service/Repository 계층 분리 대신 기능 폴더(`createxxx/`, `getxxx/`) 단위로 구성
- 자세한 규칙과 코드 컨벤션은 [`CLAUDE.md`](./CLAUDE.md) 참고 (Claude Code가 매 세션 자동으로 읽는 파일이지만, 사람이 읽어도 되는 프로젝트 규약 문서)

## 로컬 실행
```bash
# 1) 최초 1회: git hooks 연결
./scripts/setup-git-hooks.sh

# 2) 로컬 MySQL 실행 (.env 없으면 root/root/im/3306 기본값 사용)
docker compose up -d

# 3) 서버 실행
./gradlew bootRun
```
- API 문서: http://localhost:8080/swagger-ui/index.html
- 헬스체크: http://localhost:8080/actuator/health

## 배포 (운영)
- `main`에 머지되면 GitHub Actions(`.github/workflows/deploy.yml`)가 recovery-server와 **같은 EC2**에 IM-SERVER를 별도 컨테이너(포트 8090)로 배포한다 — recovery-30.shop 쪽 설정은 건드리지 않는다
- 아직 도메인이 없어 `http://<EC2 IP>:8090`으로 직접 접근한다 (Swagger UI: `http://<EC2 IP>:8090/swagger-ui/index.html`)
- recovery-server의 blue/green 무중단 배포와 달리 단일 컨테이너 배포라 재배포 중 짧은 다운타임이 있다
- 처음 배포하기 전에 필요한 것(코드로는 안 되는 부분): GitHub Actions secrets 등록, EC2 보안그룹 8090 포트 오픈 — 자세한 목록은 `CLAUDE.md`의 "인프라 / 배포" 절 참고

## 빌드 / 테스트
```bash
./gradlew build            # 빌드
./gradlew test             # 테스트 (ArchUnit 아키텍처 규칙 포함)
./gradlew spotlessApply    # 포맷팅 (커밋 전 필수)
```

## 브랜치 / 커밋 규칙
- `main` 직접 작업 금지, `./scripts/new-feature-branch.sh <기능명>` 으로 `feature/<기능명>` 브랜치 생성
- 커밋 메시지: `[타입] 설명` (feat, fix, refactor, domain, arch, docs, test, chore)

## 진행 현황 / TODO
- [x] Spring Boot 4.1.1 + Java 25 스캐폴딩 (Spring Initializr)
- [x] 모듈러 모놀리스 + 버티컬 슬라이스 아키텍처 규칙 정리 (CLAUDE.md)
- [x] 공통 응답 포맷 / 예외 처리 (`shared/response`, `shared/exception`)
- [x] CORS / Swagger 설정 (`shared/web`)
- [x] ArchUnit 아키텍처 테스트 뼈대 (도메인 모듈 생기면 규칙 추가)
- [x] 로컬 개발용 Docker Compose(MySQL) + Dockerfile
- [x] GitHub Actions CI (spotlessCheck + test)
- [x] git hooks (커밋 메시지 검사, main 직접/강제 push 차단)
- [x] PRD v1.0 정리 (`docs/prd.md`)
- [x] S01·S02 와이어프레임 → API 설계 참고 정리 (`docs/wireframes.md`)
- [x] S01~S06·E01 전체 화면 기반 DB 설계 (`docs/db-design.md`) + 첫 Flyway 마이그레이션(`V1~V4`, 아직 엔티티는 없음)
- [x] 리소스별 API 설계 (`docs/api-design.md` — condition/comparison/decision/proof/consultation 리소스, 신규 ErrorCode 제안 포함)
- [x] `condition`/`comparison`/`decision` 모듈 구현 — V1·V2 Snapshot, 구조화 입력 비교 Rule Engine(STRUCTURED_API 경로만, AI 문서추출은 미구현), 원리금균등 계산, Review Gate, 전자서명 Decision까지 end-to-end 테스트로 검증
- [x] 서명 세션(`signature-sessions`), 비교 재시도(`:retry`) 엔드포인트 구현
- [x] `proof` 모듈 구현 — `implementation-spec.md` 13장 Blockchain Adapter Contract대로 `BlockchainAdapter` 인터페이스 + `MockBlockchainAdapter`(DB 기반 목 원장) 구현. Decision(PROCEED) 저장 시 `DecisionSubmittedEvent`로 자동 anchoring, `GET /api/proof/{proofId}`·`:verify`까지 동작 확인
- [x] `consultation` 모듈 구현 — 상담 핸드오프 기록(실제 상담 연동은 Non-goal)
- [ ] `comparison_run_steps`/`comparison_item_evidence`(AI Reason·Evidence) — **실제 AI/문서추출 서비스 연동이 필요한 범위**라 이번엔 안 함. 지금은 비교가 동기로 즉시 끝나 진행상태 UI(S02)가 항상 100%로만 보이고, S04의 "확인된 변경 사유"·원문 근거 링크는 응답에 없음
- [x] 실제 운영 RDS(`im` 스키마)에 매 배포마다 직접 붙여서 Hibernate `ddl-auto=validate` 통과 확인 — H2 테스트는 마이그레이션을 안 타므로 이 확인을 대체하지 못함(V6에서 실제로 타입 불일치를 잡은 적 있음)
- [x] 배포 파이프라인 (`deploy/`, `.github/workflows/deploy.yml`) — recovery-server와 같은 EC2, 8090 포트에 단일 컨테이너로 배포
- [x] GitHub Actions secrets 등록 + EC2 보안그룹 8090 포트 오픈 (2026-09-19 완료)
- [ ] 운영 도메인이 생기면 CORS 허용 origin 갱신 + nginx 가상호스팅으로 전환
- [ ] 인증/인가(고객·상담원·운영자·AI Worker·Ledger Writer 역할 분리, `implementation-spec.md` 14장) — 아직 없음. `proof` 검증 API의 `requestedBy`가 항상 `SYSTEM`으로 기록되는 것도 이 때문
- [ ] 실제 블록체인 네트워크 선정 시 `proof.internal.MockBlockchainAdapter`를 `proof.api.BlockchainAdapter` 새 구현체로 교체
