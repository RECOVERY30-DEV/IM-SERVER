package com.im.server.decision.internal;

import com.im.server.decision.api.DecisionApi;
import com.im.server.decision.api.DecisionView;
import com.im.server.decision.domain.Decision;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class DecisionApiImpl implements DecisionApi {

  private final DecisionRepository decisionRepository;

  public DecisionApiImpl(DecisionRepository decisionRepository) {
    this.decisionRepository = decisionRepository;
  }

  @Override
  public DecisionView getDecision(Long decisionId) {
    Decision decision =
        decisionRepository
            .findById(decisionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    return new DecisionView(
        decision.getId(),
        decision.getComparisonRunId(),
        decision.getDecisionType(),
        decision.isBypassedUncertainItems(),
        decision.isAllItemsConfirmed(),
        decision.getSignatureMethod(),
        decision.getSignedAt(),
        decision.getPostSnapshotIdAtDecision(),
        decision.getContractDocumentHashAtDecision(),
        decision.getCreatedAt());
  }
}
