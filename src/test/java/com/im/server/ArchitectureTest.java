package com.im.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 모듈(bounded context) 경계를 강제하는 아키텍처 테스트. ./gradlew test 실행 시 다른 테스트와 함께 자동으로 돌아갑니다.
 *
 * <p>규칙 요약: 1. 각 모듈의 internal 패키지는 그 모듈 밖에서 절대 참조할 수 없다. 2. 각 모듈의 domain 패키지는 그 모듈 밖에서 직접 참조할 수 없다
 * (api를 통해서만 접근).
 *
 * <p>새 모듈을 추가하면 아래 블록 하나를 그대로 복사해서 모듈명만 바꿔 추가하세요.
 */
@AnalyzeClasses(packages = "com.im.server")
public class ArchitectureTest {

  // ── condition 모듈 ──
  @ArchTest
  static final ArchRule condition_internal_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..condition..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..condition.internal..")
          .because("condition.internal은 condition 모듈 내부에서만 사용해야 합니다");

  @ArchTest
  static final ArchRule condition_domain_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..condition..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..condition.domain..")
          .because("condition.domain은 condition 모듈 밖에서 직접 참조할 수 없습니다 (condition.api를 통해서만 접근)");

  // ── comparison 모듈 ──
  @ArchTest
  static final ArchRule comparison_internal_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..comparison..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..comparison.internal..")
          .because("comparison.internal은 comparison 모듈 내부에서만 사용해야 합니다");

  @ArchTest
  static final ArchRule comparison_domain_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..comparison..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..comparison.domain..")
          .because("comparison.domain은 comparison 모듈 밖에서 직접 참조할 수 없습니다 (comparison.api를 통해서만 접근)");

  // ── decision 모듈 ── (아직 api 패키지가 없음 — 다른 모듈이 필요로 할 때 추가)
  @ArchTest
  static final ArchRule decision_internal_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..decision..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..decision.internal..")
          .because("decision.internal은 decision 모듈 내부에서만 사용해야 합니다");

  @ArchTest
  static final ArchRule decision_domain_is_not_accessed_from_outside =
      noClasses()
          .that()
          .resideOutsideOfPackage("..decision..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..decision.domain..")
          .because("decision.domain은 decision 모듈 밖에서 직접 참조할 수 없습니다");
}
