package com.im.server.comparison.getcomparisonrun;

import com.im.server.comparison.domain.ComparisonRun;
import com.im.server.comparison.internal.ComparisonRunRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * S02(비교 진행 중)/S03 최초 로드용 상태 조회. 이 서버는 비교를 동기로 즉시 완료시키므로(내부 8단계를 실시간 Job으로 분리하지 않음) 조회 시점엔 항상 완료
 * 상태다 — 진행률 폴링 UI는 항상 100%를 받게 된다. 장시간 비동기 파이프라인(AI 문서추출 등)이 붙으면 이 Handler를 실제 진행 중 상태를 반환하도록 확장해야
 * 한다.
 */
@RestController
@Tag(name = "comparison", description = "V1·V2 비교 결과")
public class GetComparisonRunHandler {

  private final ComparisonRunRepository comparisonRunRepository;

  public GetComparisonRunHandler(ComparisonRunRepository comparisonRunRepository) {
    this.comparisonRunRepository = comparisonRunRepository;
  }

  @Operation(summary = "비교 Job 상태 조회", description = "S02 진행상태, S03 진입 시 Overall Status 확인에 쓴다.")
  @GetMapping("/api/comparisons/{comparisonId}")
  public ResponseEntity<ApiResponse<ComparisonRunResponse>> handle(
      @PathVariable Long comparisonId) {
    ComparisonRun run =
        comparisonRunRepository
            .findById(comparisonId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_RUN_NOT_FOUND));

    ComparisonProgressResponse progress =
        new ComparisonProgressResponse(
            run.getProgressPercent(), run.getCompletedSteps(), run.getTotalSteps());

    return ResponseEntity.ok(
        ApiResponse.success(
            new ComparisonRunResponse(
                run.getId(),
                run.getApplicationId(),
                run.getStatus(),
                run.getOverallStatus(),
                run.getUncertainReason(),
                progress)));
  }
}
