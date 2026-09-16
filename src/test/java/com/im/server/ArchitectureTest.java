package com.im.server;

import com.tngtech.archunit.junit.AnalyzeClasses;

/**
 * 모듈(bounded context) 경계를 강제하는 아키텍처 테스트. ./gradlew test 실행 시 다른 테스트와 함께 자동으로 돌아갑니다.
 *
 * <p>규칙 요약: 1. 각 모듈의 internal 패키지는 그 모듈 밖에서 절대 참조할 수 없다. 2. 각 모듈의 domain 패키지는 그 모듈 밖에서 직접 참조할 수 없다
 * (api를 통해서만 접근).
 *
 * <p>아직 도메인 모듈이 없어 규칙이 비어있습니다. 첫 모듈(예: member)을 추가하면 아래 블록을 복사해서 모듈명만 바꿔 추가하세요.
 *
 * <pre>{@code
 * @ArchTest
 * static final ArchRule member_internal_is_not_accessed_from_outside =
 *     noClasses()
 *         .that()
 *         .resideOutsideOfPackage("..member..")
 *         .should()
 *         .dependOnClassesThat()
 *         .resideInAPackage("..member.internal..")
 *         .because("member.internal은 member 모듈 내부에서만 사용해야 합니다");
 *
 * @ArchTest
 * static final ArchRule member_domain_is_not_accessed_from_outside =
 *     noClasses()
 *         .that()
 *         .resideOutsideOfPackage("..member..")
 *         .should()
 *         .dependOnClassesThat()
 *         .resideInAPackage("..member.domain..")
 *         .because("member.domain은 member 모듈 밖에서 직접 참조할 수 없습니다 (member.api를 통해서만 접근)");
 * }</pre>
 */
@AnalyzeClasses(packages = "com.im.server")
public class ArchitectureTest {}
