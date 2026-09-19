package com.im.server.comparison.retrycomparison;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.condition.api.ConditionApi;
import com.im.server.condition.api.PostConditionSnapshotView;
import com.im.server.condition.api.PreConditionSnapshotView;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UNCERTAIN 상태에서 재추출을 다시 시도하거나, 계약서 Version이 바뀌어 재비교가 필요할 때(FR03) 최신 V1·V2로 새 비교 Job을 만든다. 같은
 * Snapshot·정책 Version 조합이 이미 있으면 {@code startComparison}이 {@code COMPARISON_409_2}를 던진다.
 */
@RestController
@Tag(name = "comparison", description = "V1·V2 비교 결과")
public class RetryComparisonHandler {

  private final ConditionApi conditionApi;
  private final ComparisonApi comparisonApi;

  public RetryComparisonHandler(ConditionApi conditionApi, ComparisonApi comparisonApi) {
    this.conditionApi = conditionApi;
    this.comparisonApi = comparisonApi;
  }

  @Operation(summary = "비교 재실행", description = "최신 V1·V2로 새 비교 Job을 만든다.")
  @PostMapping("/api/applications/{applicationId}/comparisons:retry")
  public ResponseEntity<ApiResponse<RetryComparisonResponse>> handle(
      @PathVariable String applicationId) {
    PreConditionSnapshotView preSnapshot = conditionApi.getLatestPreSnapshot(applicationId);
    PostConditionSnapshotView postSnapshot = conditionApi.getLatestPostSnapshot(applicationId);

    Long comparisonId =
        comparisonApi.startComparison(applicationId, preSnapshot.id(), postSnapshot.id());

    return ResponseEntity.ok(ApiResponse.success(new RetryComparisonResponse(comparisonId)));
  }
}
