# IM-SERVER

## 프로젝트 개요
- 서비스명: **iM 조건체크** — 사전 안내 대출조건(V1)과 최종 약정조건(V2)을 전자서명 직전에 자동 대조하고, 달라진 조건·금전적 영향·원문 근거를 고객이 확인한 뒤 그 비교 기록의 무결성까지 검증할 수 있게 하는 서비스
- 제품 배경/문제정의/요구사항/정책은 [`docs/prd.md`](./docs/prd.md) (PRD v1.0, 2026-09-12 기준)가 **정본**이다 — 새 기능을 설계하기 전에 반드시 먼저 읽을 것
- 화면 매핑은 [`docs/wireframes.md`](./docs/wireframes.md), DB 스키마는 [`docs/db-design.md`](./docs/db-design.md), 리소스별 API 스펙(Command/Query/Response 필드, 에러코드)은 [`docs/api-design.md`](./docs/api-design.md)가 정본 — 슬라이스 구현 시 이 문서의 엔드포인트/필드명을 그대로 따를 것
- 백엔드 서버 (Spring Boot, 모듈러 모놀리스 아키텍처)
- 언어: Java 25
- 빌드 도구: Gradle
- 아키텍처 스타일: 모듈 = Bounded Context, 모듈 내부 = Vertical Slice(기능 단위)
- 이 저장소는 자매 프로젝트 [recovery-server](https://github.com/RECOVERY30-DEV)와 동일한 아키텍처/컨벤션을 쓴다. 두 프로젝트의 CLAUDE.md가 갈라지면 각자 알아서 업데이트할 것 — override 관계가 아니라 별도 저장소다.
- 2026-09-16 기준: 아직 도메인 모듈이 하나도 없는 초기 세팅 상태. 아래 규칙은 첫 모듈을 추가할 때부터 적용된다.
- `docs/prd.md`의 Non-goals(6.5 대출 추천/승인/신용평가/실계좌 연동/On-chain 원문 저장 등)는 이 서버가 절대 구현하지 않는 범위이니 착각하지 말 것

## 아키텍처 원칙 (반드시 지킬 것)

### 1. 모듈은 서로 직접 참조하지 않는다
- 모듈 간 참조는 오직 두 경로로만 허용:
    - **동기 필요 시** → 상대 모듈의 `api/` 패키지에 있는 인터페이스만 호출 (구현체·internal 패키지는 절대 참조 금지)
    - **비동기 가능 시** → `shared/event/`의 이벤트를 발행하고 다른 모듈이 구독
- `internal/` 패키지에 있는 클래스는 그 모듈 밖에서 import하면 안 됨
- 새 기능 추가할 때 다른 모듈의 `internal`이나 `domain`을 직접 import하려는 코드가 보이면 즉시 지적하고, `api` 인터페이스나 이벤트로 우회할 것

### 2. 모듈 내부는 레이어가 아니라 슬라이스(기능) 단위로 구성한다
- 전통적인 Controller/Service/Repository 계층 분리 금지
- 기능 하나 = 폴더 하나 (예: `createorder/`, `getorder/`)
- 슬라이스 폴더 안에는 그 기능에 필요한 것만: `Command/Query`(입력), `Handler`(컨트롤러+서비스+레포 로직 전체), `Response/View`(출력)
- 기능을 수정할 때 여러 패키지를 오가지 않고 슬라이스 폴더 하나만 보면 되도록 유지

### 3. 경계는 코드 리뷰가 아니라 도구로 강제한다
- ArchUnit으로 다음 규칙을 테스트에 포함:
    - 모듈 패키지가 다른 모듈의 `internal` 또는 `domain`을 참조하면 빌드 실패
    - `api` 패키지 외부로 구현 클래스가 노출되지 않는지 검증
- 새 모듈이나 슬라이스를 추가할 때 `src/test/java/com/im/server/ArchitectureTest.java`에 그 모듈의 규칙 블록을 복사해서 추가할 것 (파일 상단 주석에 템플릿 있음)

## 폴더 구조
```
src/main/java/com/im/server
├── condition/                  ← V1/V2 Snapshot (FR01~FR03)
│   ├── api/                    ← ConditionApi, ConditionFields, ConditionFieldCode 등
│   ├── domain/                  ← PreConditionSnapshot, PostConditionSnapshot
│   ├── savepreconditionsnapshot/  ← 슬라이스: Command/Handler/Response
│   ├── getlatestprecondition/
│   ├── listpreconditions/
│   ├── savepostconditionsnapshot/
│   └── internal/                ← Repository, ConditionApiImpl
├── comparison/                 ← 비교·계산 엔진 (FR04~FR09)
│   ├── api/                     ← ComparisonApi, ComparisonRunView, ComparisonItemView 등
│   ├── domain/                   ← ComparisonRun, ComparisonItem, ComparisonImpact
│   ├── getcomparisonrun/
│   ├── getcomparisonsummary/
│   ├── listcomparisonitems/
│   ├── getcomparisonitem/
│   └── internal/                 ← ComparisonEngine(Rule), EqualInstallmentCalculator(Calculation), Repository, ComparisonApiImpl
├── decision/                   ← Review Gate·서명세션·전자서명 (FR11, FR12)
│   ├── api/                     ← DecisionApi, DecisionView, DecisionType
│   ├── domain/                   ← DecisionRequiredReview, Decision, SignatureSession
│   ├── getreviewgate/
│   ├── reviewcomparisonitem/
│   ├── issuesignaturesession/
│   ├── submitdecision/           ← PROCEED 저장 시 DecisionSubmittedEvent 발행 (proof 모듈이 구독)
│   └── internal/                 ← Repository, ReviewGateEvaluator, DecisionApiImpl
├── proof/                      ← 무결성 증빙 (FR13, implementation-spec.md 13장)
│   ├── api/                     ← BlockchainAdapter 계약, AnchorStatus, LedgerStatus 등
│   ├── domain/                   ← ProofRecord, ProofVerification
│   ├── getproofstatus/           ← S06 요약(GET /decisions/{id}/proof)
│   ├── getproofdetail/           ← "기록 자세히 보기"(GET /proof/{proofId})
│   ├── verifyproof/              ← 재검증(POST /proof/{proofId}:verify)
│   └── internal/                 ← MockBlockchainAdapter(목 원장), ProofAnchoringService,
│                                    CanonicalProofPayloadFactory, DecisionSubmittedEventListener
├── consultation/               ← 상담 핸드오프 (경량, 실제 상담 연동은 Non-goal)
│   ├── domain/                   ← ConsultationReferral
│   ├── createconsultationreferral/
│   └── internal/                 ← Repository
└── shared/                    ← 공유 커널
    ├── event/                 ← 모듈 간 비동기 통신 — DecisionSubmittedEvent(decision → proof)가 첫 실사용 사례
    ├── response/               ← 공통 응답 포맷 (ApiResponse, ApiError)
    ├── exception/              ← 공통 예외 체계 (BusinessException, ErrorCode, GlobalExceptionHandler)
    ├── util/                   ← CanonicalJson (Hash용 canonical 직렬화)
    └── web/                    ← 전역 web 설정 (CorsConfig, OpenApiConfig, RootController)
```

## 새 기능 추가 시 작업 순서
1. 어느 모듈(bounded context)에 속하는지 먼저 판단 (아직 없으면 새 모듈부터 생성)
2. 기존 슬라이스에 끼워 넣을지, 새 슬라이스 폴더를 만들지 결정 (다른 기능과 로직을 공유하려는 유혹이 들면 대부분 새 슬라이스가 맞음)
3. 다른 모듈 데이터가 필요하면 해당 모듈의 `api` 인터페이스 확인 → 없으면 그 모듈에 인터페이스를 먼저 추가
4. Command/Query → Handler → Response 순으로 작성
5. `./gradlew test` (ArchUnit 규칙 포함) 통과 확인 후 커밋

## 코드 컨벤션

### DTO / 도메인 객체
- `Command`/`Query`/`Response`/`View`는 Java record로 작성 (Lombok 사용 안 함)
- 엔티티·값 객체(`domain/` 패키지)는 `@Getter @Setter @NoArgsConstructor` + 별도 생성자에서 유효성 검증, 실패 시 `BusinessException` throw

### Jackson 3 주의 (Spring Boot 4)
- 이 프로젝트의 `ObjectMapper`/`JsonNode`/`MapperFeature` 등은 **`tools.jackson.databind.*`** (Jackson 3)다 — `com.fasterxml.jackson.databind.*`(Jackson 2, "Classic")를 import하면 컴파일은 되지만 Spring이 자동구성한 Bean과 타입이 달라 `NoSuchBeanDefinitionException`으로 기동이 실패한다
- `com.fasterxml.jackson.annotation.*`(`@JsonProperty` 등 어노테이션)는 여전히 Jackson 2 패키지를 그대로 쓴다 — 어노테이션과 런타임 타입(ObjectMapper 등)의 groupId가 다르다는 점을 헷갈리지 말 것
- 커스텀 `ObjectMapper`가 필요하면 생성자로 설정을 바꿀 수 없다(불변) — `JsonMapper.builder().enable(...).disable(...).build()`로 만들 것 (참고: `shared/util/CanonicalJson`)
- `writeValueAsString`/`readValue`가 던지는 `JacksonException`은 Jackson 2와 달리 **unchecked**다

### 응답 포맷
- 모든 컨트롤러(Handler)는 `ResponseEntity<ApiResponse<T>>`를 반환한다
- 성공: `ApiResponse.success(data)`
- 실패는 직접 만들지 않는다 — `BusinessException(ErrorCode.XXX)`를 던지면 `GlobalExceptionHandler`가 `ApiResponse.error(...)`로 변환해서 내려준다
- 새 에러가 필요하면 `shared/exception/ErrorCode`에 상수 추가. 코드 네이밍: `{모듈}_{HTTP상태}_{순번}` (예: `MEMBER_400_2`), 공통 에러는 `COMMON_` 접두사

### API 문서화 (Swagger / springdoc-openapi)
- 슬라이스 구조상 엔드포인트마다 클래스가 따로이므로, `@Tag(name = "모듈명", description = "...")`을 그 모듈의 모든 Handler 클래스에 동일하게 붙여서 Swagger UI에서 한 그룹으로 묶는다
- 메서드에 `@Operation(summary = "...", description = "...")`을 한국어로 작성
- `@ApiResponses`로 성공/실패 상태코드를 명시하고, 실패 응답은 `content = @Content(schema = @Schema(implementation = ApiError.class))`로 에러 스키마를 참조한다 (`io.swagger.v3.oas.annotations.responses.ApiResponse`는 우리 `ApiResponse`와 이름이 겹치므로 완전한 패키지 경로로 사용)
- Command/Response/View record의 각 필드에 `@Schema(description = "...", example = "...")` 추가

### DB / 마이그레이션
- 스키마는 [`docs/db-design.md`](./docs/db-design.md)가 정본이다 — 새 테이블을 설계하기 전에 먼저 확인할 것 (테이블 접두사 = 모듈, 3장에 컬럼별 근거)
- 스키마는 Flyway가 관리한다 (`spring.jpa.hibernate.ddl-auto=validate`) — 엔티티만 고치고 마이그레이션을 안 만들면 애플리케이션이 기동 실패한다
- 새 테이블/컬럼이 필요하면 `src/main/resources/db/migration/V{n}__{설명}.sql` 추가 (다음 버전 번호는 기존 파일 중 가장 큰 `V{n}` + 1, 현재 `V6`까지 존재)
- **엔티티 필드 타입과 마이그레이션 컬럼 타입을 정확히 맞출 것** — 특히 숫자류(`SMALLINT`/`INT`/`BIGINT`)는 Hibernate `validate`가 엄격하게 검사해서 하나만 안 맞아도 기동 자체가 실패한다(운영 배포 중 `comparison_runs.progress_percent`가 `SMALLINT`인데 엔티티는 `int`→`INTEGER`라 실패했던 사례가 `V6`). `VARCHAR` 길이나 `CHAR`/`VARCHAR` 차이, `BigDecimal`의 명시 안 한 precision/scale은 이 프로젝트 Hibernate 버전에서는 관대하게 통과하더라 — 그래도 숫자 타입만큼은 항상 실제 MySQL로 확인할 것
- **로컬 H2 테스트(`ddl-auto=create-drop`)는 Flyway 마이그레이션을 아예 안 타서 위 같은 불일치를 못 잡는다** — 새 컬럼을 추가했으면 최소한 한 번은 `docker compose up -d`로 띄운 실제 MySQL에 `SPRING_PROFILES_ACTIVE` 없이(즉 `application.properties` 그대로) 붙여서 기동이 되는지 확인할 것
- 로컬 개발 DB는 `docker compose up -d` (MySQL, `.env` 없으면 root/root/im/3306 기본값 사용)
- 클라우드 DB에 직접 붙어야 할 때만 `application-local.yml`을 만들어 쓴다 (gitignore 대상, `SPRING_PROFILES_ACTIVE=local`로 활성화)

### 테스트
- 슬라이스 테스트는 `@SpringBootTest @AutoConfigureMockMvc @Transactional` + `MockMvc`로 API를 직접 호출하는 통합테스트 스타일로 작성한다 (컨트롤러/서비스 목킹해서 쪼개지 않음)
- 테스트 메서드명은 한국어로 `조건_결과()` 형태 (예: `이메일_형식이_잘못되면_400과_에러코드를_반환한다`)
- 응답 검증은 `ApiResponse` 포맷 그대로 확인: `jsonPath("$.success")`, 성공 시 `$.data.*`, 실패 시 `$.error.code`
- 도메인 객체(값 객체 등)의 검증 로직은 별도 단위테스트로

### 포맷팅
- 커밋 전 `./gradlew spotlessApply` (googleJavaFormat 기준). CI에서 `spotlessCheck`로 검증하므로 안 돌리면 PR이 실패한다
- spotless 대상은 `src/**/*.java`만 — QueryDSL이 생성하는 `build/generated/querydsl`은 제외되어 있음

## 인프라 / 배포
- **운영 주소: `https://im.recovery-30.shop`** (2026-09-20 연결 완료). Swagger UI `https://im.recovery-30.shop/swagger-ui/index.html`, 헬스체크 `https://im.recovery-30.shop/actuator/health`
- **recovery-server와 같은 EC2**에 올린다. recovery-server는 그 EC2에서 nginx + blue/green(8080/8081)으로 `recovery-30.shop`을 서비스하고 있고, 이 서버는 건드리지 않는다 — IM-SERVER는 같은 EC2에 **별도 컨테이너(내부 포트 8090)** 로 뜨고, nginx가 `server_name` 기반 가상호스팅으로 `im.recovery-30.shop` 요청만 그 컨테이너로 넘긴다
- nginx 설정은 EC2의 `/etc/nginx/conf.d/im-service.conf`에 있다(참고용 사본: `deploy/nginx/im-service.conf` — **이 저장소의 배포 자동화 대상이 아니라 수동 반영 필요**, 고치면 `scp` 후 EC2에서 `sudo nginx -t && sudo nginx -s reload`). recovery-30.shop의 `service.conf`와 별도 파일이라 서로 독립적으로 수정 가능하지만, `listen [::]:443 ssl ipv6only=on;`처럼 프로세스 전체에 한 번만 선언 가능한 옵션이 있으니 새 서버 블록 추가 시 주의(중복 시 `nginx -t`가 `duplicate listen options`로 막아줌)
- SSL 인증서는 `certbot certonly --nginx -d im.recovery-30.shop`으로 recovery-30.shop과 **별도 발급**했다(`/etc/letsencrypt/live/im.recovery-30.shop/`) — 자동 갱신 대상에 포함됨. DNS는 `im.recovery-30.shop` A레코드 → EC2 IP를 도메인 등록기관(Route53 아님, 이 프로젝트 AWS IAM 계정엔 Route53 권한 없음)에서 직접 추가했음
- recovery-server와 달리 **블루그린이 아니다** — `deploy/deploy.sh`는 컨테이너를 그 자리에서 교체하는 단일 컨테이너 배포라 재배포 중 짧은 다운타임이 있고, 헬스체크 실패 시 자동 롤백도 없다(수동으로 이전 TAG 재배포 필요). 트래픽이 늘거나 무중단이 필요해지면 recovery-server의 blue/green 패턴으로 승격할 것
- `.github/workflows/deploy.yml`이 `main` push 시 Docker Hub(`haul123/im-server`)로 이미지를 빌드/푸시하고 EC2에 SSH로 배포한다(nginx 설정 자체는 건드리지 않음 — 컨테이너만 재기동). GitHub Actions secrets(`DOCKER_USERNAME`, `DOCKER_PASSWORD`, `EC2_HOST`, `EC2_SSH_KEY`, `IM_DB_HOST`, `IM_DB_PORT`, `IM_DB_NAME`, `IM_DB_USERNAME`, `IM_DB_PASSWORD`)와 EC2 보안그룹 8090 포트 인바운드는 이미 등록·오픈되어 있음(2026-09-19)
- 프론트엔드 주소가 정해지면 `application.properties`의 `app.cors.allowed-origins` 기본값에 추가하거나 배포 시 `CORS_ALLOWED_ORIGINS` 환경변수로 덮어쓸 것 (지금은 로컬 개발 주소만 허용)
- Swagger UI는 기본적으로 인증 없이 열려 있다 — 실제 서비스 전환 시 `springdoc.swagger-ui.enabled=false` 등으로 막을 것

## 빌드 / 실행 명령
```bash
./gradlew build            # 빌드
./gradlew test             # 테스트 (ArchUnit 아키텍처 규칙 포함)
./gradlew bootRun          # 로컬 서버 실행
```

## 커밋 / PR 규칙
- 커밋 메시지: `[타입] 설명` (예: `[feat] 주문 생성 슬라이스 추가`)
- 타입: feat, fix, refactor, domain, arch(아키텍처 규칙 변경), docs, test, chore
- 브랜치: `main` 직접 작업 금지, `feature/기능명` 브랜치 사용
- 모듈 경계를 넘는 변경(공유 커널 수정, api 인터페이스 변경)은 PR 설명에 영향받는 모듈 명시

## Git 훅 설정 (최초 1회, 클론 직후)
```bash
./scripts/setup-git-hooks.sh    # core.hooksPath를 .githooks 로 지정
```
- `.githooks/commit-msg`: 커밋 메시지가 `[타입] 설명` 형식이 아니면 커밋 자체를 거부
- `.githooks/pre-push`: `main`/`master`로의 직접 push, 강제(non-fast-forward) push를 거부
- 새 기능 브랜치는 `./scripts/new-feature-branch.sh <기능명>` 으로 생성 (`main`에서 최신화 후 `feature/<기능명>` 생성)
- 훅 스크립트를 수정하면 팀원 전체가 다시 pull 받아야 적용됨 (강제 배포 수단은 아님, 최초 설정을 각자 1회 실행해야 함)

## Claude 자동 커밋 · 푸시 · PR 정책
- `feature/*` 브랜치에서 커밋되지 않은 변경사항이 남아 있으면(Stop 훅이 `scripts/check-uncommitted.sh`로 감지), Claude는 사용자에게 다시 묻지 않고 아래를 자동으로 수행한다:
  1. 변경 내용 검토 (민감정보 파일은 절대 add하지 않음)
  2. `[타입] 설명` 컨벤션으로 커밋
  3. `git push -u origin <현재 브랜치>`
  4. `gh pr view`으로 해당 브랜치 PR 존재 확인 → 없으면 `gh pr create --fill`로 생성, 있으면 push만으로 갱신
- `main`/`master`에서는 이 자동화가 동작하지 않음 (훅이 스킵) — `main`은 항상 사람이 직접 판단해서 다룰 것
- 이 정책은 사용자가 명시적으로 요청한 것이며, 매번 push/PR 생성 전에 확인받지 않아도 됨

## 절대 하지 말 것
- 다른 모듈의 `internal/`, `domain/` 패키지 직접 import
- 레이어드 구조로 되돌리기 (Controller/Service/Repository 패키지 분리)
- `.env`, `application-secret.yml` 등 민감 정보 파일 읽기/커밋
- `main` 브랜치 강제 push
- ArchUnit 규칙 비활성화하거나 우회

## 모듈별 CLAUDE.md (모듈 컨텍스트가 쌓이면 분리)
- 이 루트 `CLAUDE.md`는 **모든 모듈에 공통인 규칙만** 담는다 (아키텍처 원칙, 응답 포맷, 커밋 규칙 등)
- 어떤 모듈이 자기만의 고유한 맥락(외부 API 연동 방식, 그 모듈만의 특수한 도메인 규칙/제약, 별도 인프라 의존성 등)을 갖게 되면, 매번 파악하지 않도록 그 모듈 폴더에 `CLAUDE.md`를 새로 만들어 옮겨 적는다
  - 예: `src/main/java/com/im/server/{모듈명}/CLAUDE.md`
  - 이 파일은 그 모듈 안의 파일을 실제로 열어서 작업할 때만 자동으로 로드된다 (다른 모듈 작업 중엔 로드 안 됨) — 그래서 모듈이 늘어나도 세션마다 불필요한 내용까지 다 읽지 않는다
  - 새 모듈을 처음 만들 때는 아직 쌓인 맥락이 없으니 억지로 만들 필요 없음. "다음에 또 설명해야 할 것 같다" 싶은 내용이 생기는 시점에 만들 것
- 여러 계층의 `CLAUDE.md`는 override가 아니라 합쳐져서 로드된다 (루트 내용 + 해당 모듈 내용)

## 참고 사항
- 이 파일은 매 세션 시작 시 자동으로 로드됩니다.
- 모듈이 추가되거나 아키텍처 규칙이 바뀌면 이 파일도 같이 업데이트하세요.
