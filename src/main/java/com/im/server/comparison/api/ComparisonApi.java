package com.im.server.comparison.api;

import java.util.List;

/** comparison 모듈 밖(condition·decision 등)에서 접근하는 유일한 통로. */
public interface ComparisonApi {

  /** V2 저장 직후 condition 모듈이 호출한다 — 비교를 동기 실행하고 comparisonRunId를 반환한다. */
  Long startComparison(String applicationId, Long preSnapshotId, Long postSnapshotId);

  ComparisonRunView getRun(Long comparisonRunId);

  ComparisonItemView getItem(Long itemId);

  List<ComparisonItemView> listRequiredReviewItems(Long comparisonRunId);
}
