package com.im.server.comparison.getcomparisonitem;

import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.comparison.internal.ComparisonItemPresenter;
import com.im.server.comparison.internal.ComparisonItemRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** S04 항목 상세 조회. */
@RestController
@Tag(name = "comparison", description = "V1·V2 비교 결과")
public class GetComparisonItemHandler {

  private final ComparisonItemRepository comparisonItemRepository;

  public GetComparisonItemHandler(ComparisonItemRepository comparisonItemRepository) {
    this.comparisonItemRepository = comparisonItemRepository;
  }

  @Operation(summary = "비교 항목 상세 조회", description = "Before/After/변화량/확인 필요 여부를 반환한다.")
  @GetMapping("/api/comparisons/items/{itemId}")
  public ResponseEntity<ApiResponse<ComparisonItemDetailResponse>> handle(
      @PathVariable Long itemId) {
    ComparisonItem item =
        comparisonItemRepository
            .findById(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_ITEM_NOT_FOUND));

    return ResponseEntity.ok(
        ApiResponse.success(
            new ComparisonItemDetailResponse(
                item.getId(),
                item.getFieldCode(),
                ComparisonItemPresenter.label(item.getFieldCode()),
                item.getItemStatus(),
                item.getV1ValueText(),
                item.getV2ValueText(),
                ComparisonItemPresenter.deltaLabel(item),
                item.isRequiresReview(),
                item.getUnknownReason())));
  }
}
