package com.im.server.comparison.getcomparisonsummary;

import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.ItemStatus;
import com.im.server.comparison.domain.ComparisonImpact;
import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.comparison.domain.ComparisonRun;
import com.im.server.comparison.internal.ComparisonImpactRepository;
import com.im.server.comparison.internal.ComparisonItemPresenter;
import com.im.server.comparison.internal.ComparisonItemRepository;
import com.im.server.comparison.internal.ComparisonRunRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** S03(변경 요약) 전용 조회. */
@RestController
@Tag(name = "comparison", description = "V1·V2 비교 결과")
public class GetComparisonSummaryHandler {

  private final ComparisonRunRepository comparisonRunRepository;
  private final ComparisonItemRepository comparisonItemRepository;
  private final ComparisonImpactRepository comparisonImpactRepository;

  public GetComparisonSummaryHandler(
      ComparisonRunRepository comparisonRunRepository,
      ComparisonItemRepository comparisonItemRepository,
      ComparisonImpactRepository comparisonImpactRepository) {
    this.comparisonRunRepository = comparisonRunRepository;
    this.comparisonItemRepository = comparisonItemRepository;
    this.comparisonImpactRepository = comparisonImpactRepository;
  }

  @Operation(summary = "변경 요약 조회", description = "Overall Status, 비용 영향, 대표 변경 항목을 한 번에 반환한다.")
  @GetMapping("/api/comparisons/{comparisonId}/summary")
  public ResponseEntity<ApiResponse<ComparisonSummaryResponse>> handle(
      @PathVariable Long comparisonId) {
    ComparisonRun run =
        comparisonRunRepository
            .findById(comparisonId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_RUN_NOT_FOUND));
    if (run.getStatus() != ComparisonRunStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.COMPARISON_RUN_NOT_COMPLETED);
    }

    List<ComparisonItem> items = comparisonItemRepository.findByComparisonRunId(comparisonId);
    ComparisonImpact impact =
        comparisonImpactRepository
            .findByComparisonRunId(comparisonId)
            .orElseThrow(() -> new IllegalStateException("완료된 비교에 impact가 없습니다: " + comparisonId));

    long changed = items.stream().filter(i -> i.getItemStatus() != ItemStatus.SAME).count();
    long unchanged = items.size() - changed;
    long requiredReview = items.stream().filter(ComparisonItem::isRequiresReview).count();

    List<HeadlineItemResponse> headline =
        items.stream()
            .filter(ComparisonItem::isRequiresReview)
            .map(
                item ->
                    new HeadlineItemResponse(
                        item.getId(),
                        item.getFieldCode(),
                        ComparisonItemPresenter.label(item.getFieldCode()),
                        item.getItemStatus(),
                        ComparisonItemPresenter.deltaLabel(item)))
            .toList();

    ComparisonImpactResponse impactResponse =
        new ComparisonImpactResponse(
            impact.getMonthlyPaymentDelta(),
            impact.getTotalInterestDelta(),
            impact.getFixedFeeDelta(),
            impact.getTotalCostDelta());

    return ResponseEntity.ok(
        ApiResponse.success(
            new ComparisonSummaryResponse(
                run.getOverallStatus(),
                impactResponse,
                (int) changed,
                (int) unchanged,
                (int) requiredReview,
                headline)));
  }
}
