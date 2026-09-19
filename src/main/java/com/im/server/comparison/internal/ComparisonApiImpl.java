package com.im.server.comparison.internal;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonItemView;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.comparison.api.OverallStatus;
import com.im.server.comparison.domain.ComparisonImpact;
import com.im.server.comparison.domain.ComparisonItem;
import com.im.server.comparison.domain.ComparisonRun;
import com.im.server.condition.api.ConditionApi;
import com.im.server.condition.api.ConditionFields;
import com.im.server.condition.api.PostConditionSnapshotView;
import com.im.server.condition.api.PreConditionSnapshotView;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.util.CanonicalJson;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComparisonApiImpl implements ComparisonApi {

  private static final String RULE_VERSION = "comparison-rule-v1";
  private static final String CALCULATION_VERSION = "equal-installment-v1";

  private final ConditionApi conditionApi;
  private final ComparisonEngine comparisonEngine;
  private final ComparisonRunRepository comparisonRunRepository;
  private final ComparisonItemRepository comparisonItemRepository;
  private final ComparisonImpactRepository comparisonImpactRepository;

  public ComparisonApiImpl(
      ConditionApi conditionApi,
      ComparisonEngine comparisonEngine,
      ComparisonRunRepository comparisonRunRepository,
      ComparisonItemRepository comparisonItemRepository,
      ComparisonImpactRepository comparisonImpactRepository) {
    this.conditionApi = conditionApi;
    this.comparisonEngine = comparisonEngine;
    this.comparisonRunRepository = comparisonRunRepository;
    this.comparisonItemRepository = comparisonItemRepository;
    this.comparisonImpactRepository = comparisonImpactRepository;
  }

  @Override
  @Transactional
  public Long startComparison(String applicationId, Long preSnapshotId, Long postSnapshotId) {
    PreConditionSnapshotView preSnapshot = conditionApi.getPreSnapshot(preSnapshotId);
    PostConditionSnapshotView postSnapshot = conditionApi.getPostSnapshot(postSnapshotId);

    ComparisonRun run =
        new ComparisonRun(
            applicationId, preSnapshotId, postSnapshotId, RULE_VERSION, CALCULATION_VERSION);
    comparisonRunRepository.save(run);

    List<ComparisonItem> items =
        comparisonEngine.compare(preSnapshot.fields(), postSnapshot.fields());
    items.forEach(item -> item.setComparisonRunId(run.getId()));
    comparisonItemRepository.saveAll(items);
    run.markStep(items.size(), items.size());

    ComparisonImpact impact =
        calculateImpact(run.getId(), preSnapshot.fields(), postSnapshot.fields());
    comparisonImpactRepository.save(impact);

    OverallStatus overallStatus = comparisonEngine.computeOverallStatus(items);
    String comparisonHash =
        CanonicalJson.hashOf(
            new Object[] {
              applicationId,
              preSnapshotId,
              postSnapshotId,
              RULE_VERSION,
              CALCULATION_VERSION,
              overallStatus
            });
    run.complete(overallStatus, comparisonHash);
    comparisonRunRepository.save(run);

    return run.getId();
  }

  @Override
  public ComparisonRunView getRun(Long comparisonRunId) {
    return toRunView(findRun(comparisonRunId));
  }

  @Override
  public ComparisonItemView getItem(Long itemId) {
    ComparisonItem item =
        comparisonItemRepository
            .findById(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_ITEM_NOT_FOUND));
    return toItemView(item);
  }

  @Override
  public List<ComparisonItemView> listRequiredReviewItems(Long comparisonRunId) {
    return comparisonItemRepository
        .findByComparisonRunIdAndRequiresReviewTrue(comparisonRunId)
        .stream()
        .map(this::toItemView)
        .toList();
  }

  ComparisonRun findRun(Long comparisonRunId) {
    return comparisonRunRepository
        .findById(comparisonRunId)
        .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_RUN_NOT_FOUND));
  }

  ComparisonRunView toRunView(ComparisonRun run) {
    return new ComparisonRunView(
        run.getId(),
        run.getApplicationId(),
        run.getPreSnapshotId(),
        run.getPostSnapshotId(),
        run.getStatus(),
        run.getOverallStatus(),
        run.getUncertainReason(),
        run.getTotalSteps(),
        run.getCompletedSteps(),
        run.getProgressPercent());
  }

  ComparisonItemView toItemView(ComparisonItem item) {
    return new ComparisonItemView(
        item.getId(),
        item.getComparisonRunId(),
        item.getFieldCode(),
        ComparisonItemPresenter.label(item.getFieldCode()),
        item.getUnit(),
        item.isContributesToOverall(),
        item.getItemStatus(),
        item.getV1ValueText(),
        item.getV2ValueText(),
        ComparisonItemPresenter.deltaLabel(item),
        item.isRequiresReview(),
        item.getUnknownReason());
  }

  private ComparisonImpact calculateImpact(Long runId, ConditionFields v1, ConditionFields v2) {
    var v1Result =
        EqualInstallmentCalculator.calculate(
            v1.loanAmount(), v1.finalRatePercent(), v1.termMonths());
    var v2Result =
        EqualInstallmentCalculator.calculate(
            v2.loanAmount(), v2.finalRatePercent(), v2.termMonths());
    BigDecimal v1Fee = BigDecimal.valueOf(v1.stampTax() + v1.fixedUpfrontFees());
    BigDecimal v2Fee = BigDecimal.valueOf(v2.stampTax() + v2.fixedUpfrontFees());
    return new ComparisonImpact(
        runId,
        v1Result.monthlyPayment(),
        v2Result.monthlyPayment(),
        v1Result.totalInterest(),
        v2Result.totalInterest(),
        v2Fee.subtract(v1Fee),
        v2.repaymentMethod());
  }
}
