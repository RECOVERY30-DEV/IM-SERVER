# IM-SERVER

**iM 조건체크** — 사전 안내 대출조건(V1)과 최종 약정조건(V2)을 전자서명 직전에 자동 대조하고, 달라진 조건·금전적 영향·원문 근거를 고객이 확인한 뒤 그 비교 기록의 무결성까지 검증할 수 있게 하는 서비스의 백엔드 서버. [recovery-server](https://github.com/RECOVERY30-DEV) 프로젝트의 아키텍처 컨벤션(모듈러 모놀리스 + 버티컬 슬라이스)을 그대로 이어받아 세팅했다.

제품 배경/문제정의/요구사항은 [`docs/prd.md`](./docs/prd.md) (PRD v1.0, 2026-09-12 기준)가 정본이다.

이 저장소는 2026-09-16 기준 **뼈대만 갖춘 초기 세팅 상태**다. 아직 실제 도메인 모듈(조건 스냅샷, 비교/계산 엔진, 무결성 증빙 등)은 하나도 들어있지 않고, 공통 인프라(응답 포맷/예외 처리/DB/CI/로컬 개발 환경)만 구성되어 있다. 새 기능을 추가하기 전에 `CLAUDE.md`와 `docs/prd.md`를 먼저 읽을 것.

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
- [ ] 첫 도메인 모듈 설계 (`docs/prd.md`의 V1/V2 Snapshot, 비교(Comparison) 엔진부터 착수 예정)
- [ ] Flyway 첫 마이그레이션 (`V1__...sql`)
- [ ] 배포 파이프라인 (EC2/Docker Hub 등 운영 인프라가 정해지면 recovery-server의 `deploy.yml`/`deploy/` 구성 참고해서 추가)
- [ ] 운영 도메인 확정 후 CORS 허용 origin 갱신
