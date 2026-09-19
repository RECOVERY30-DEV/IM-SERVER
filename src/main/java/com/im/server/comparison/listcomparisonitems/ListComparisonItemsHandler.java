package com.im.server.comparison.listcomparisonitems;

import com.im.server.comparison.api.ItemStatus;
import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.comparison.internal.ComparisonItemPresenter;
import com.im.server.comparison.internal.ComparisonItemRepository;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** S03 "변경 없는 조건 N개" 펼침, S04 진입 전 목록. */
@RestController
@Tag(name = "comparison", description = "V1·V2 비교 결과")
public class ListComparisonItemsHandler {

  private final ComparisonItemRepository comparisonItemRepository;

  public ListComparisonItemsHandler(ComparisonItemRepository comparisonItemRepository) {
    this.comparisonItemRepository = comparisonItemRepository;
  }

  @Operation(
      summary = "비교 항목 목록",
      description = "status로 필터링한다(콤마 구분, 예: WORSE,STRUCTURAL_CHANGE,UNKNOWN).")
  @GetMapping("/api/comparisons/{comparisonId}/items")
  public ResponseEntity<ApiResponse<List<ComparisonItemSummaryResponse>>> handle(
      @PathVariable Long comparisonId, @RequestParam(required = false) String status) {
    Set<ItemStatus> filter = parseStatuses(status);

    List<ComparisonItemSummaryResponse> items =
        comparisonItemRepository.findByComparisonRunId(comparisonId).stream()
            .filter(item -> filter.isEmpty() || filter.contains(item.getItemStatus()))
            .map(this::toSummary)
            .toList();

    return ResponseEntity.ok(ApiResponse.success(items));
  }

  private Set<ItemStatus> parseStatuses(String status) {
    if (status == null || status.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(status.split(","))
        .map(String::trim)
        .map(ItemStatus::valueOf)
        .collect(Collectors.toSet());
  }

  private ComparisonItemSummaryResponse toSummary(ComparisonItem item) {
    return new ComparisonItemSummaryResponse(
        item.getId(),
        item.getFieldCode(),
        ComparisonItemPresenter.label(item.getFieldCode()),
        item.getItemStatus(),
        item.getV1ValueText(),
        item.getV2ValueText(),
        ComparisonItemPresenter.deltaLabel(item),
        item.isRequiresReview());
  }
}
