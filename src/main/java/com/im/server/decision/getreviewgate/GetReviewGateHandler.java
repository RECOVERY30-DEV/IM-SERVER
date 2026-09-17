package com.im.server.decision.getreviewgate;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.decision.domain.DecisionRequiredReview;
import com.im.server.decision.internal.ReviewGateSync;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** S05 진입 시 체크박스 활성화 여부, S03 UNCERTAIN 변형의 "확인 완료된 조건 N개" 판단. */
@RestController
@Tag(name = "decision", description = "Review Gate / 약정 결정 / 전자서명")
public class GetReviewGateHandler {

  private final ComparisonApi comparisonApi;
  private final ReviewGateSync reviewGateSync;

  public GetReviewGateHandler(ComparisonApi comparisonApi, ReviewGateSync reviewGateSync) {
    this.comparisonApi = comparisonApi;
    this.reviewGateSync = reviewGateSync;
  }

  @Operation(summary = "Review Gate 조회", description = "필수 확인 항목과 전체 확인 완료 여부를 반환한다.")
  @GetMapping("/api/comparisons/{comparisonId}/review-gate")
  public ResponseEntity<ApiResponse<ReviewGateResponse>> handle(@PathVariable Long comparisonId) {
    List<ComparisonItemView> requiredItems = comparisonApi.listRequiredReviewItems(comparisonId);
    List<DecisionRequiredReview> reviews = reviewGateSync.ensure(comparisonId, requiredItems);

    Map<Long, DecisionRequiredReview> byItemId =
        reviews.stream()
            .collect(
                Collectors.toMap(DecisionRequiredReview::getComparisonItemId, Function.identity()));

    List<RequiredReviewItemResponse> items =
        requiredItems.stream()
            .map(
                item ->
                    new RequiredReviewItemResponse(
                        item.id(), item.label(), byItemId.get(item.id()).isReviewed()))
            .toList();

    boolean allReviewed = items.stream().allMatch(RequiredReviewItemResponse::reviewed);

    return ResponseEntity.ok(ApiResponse.success(new ReviewGateResponse(items, allReviewed)));
  }
}
