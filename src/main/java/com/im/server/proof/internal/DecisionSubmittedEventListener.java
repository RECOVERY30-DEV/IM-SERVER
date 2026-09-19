package com.im.server.proof.internal;

import com.im.server.shared.event.DecisionSubmittedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** decision 모듈은 proof 모듈의 존재를 모른다 — 이 리스너가 이벤트를 구독해서 반응한다(CLAUDE.md 아키텍처 원칙 1). */
@Component
public class DecisionSubmittedEventListener {

  private final ProofAnchoringService proofAnchoringService;

  public DecisionSubmittedEventListener(ProofAnchoringService proofAnchoringService) {
    this.proofAnchoringService = proofAnchoringService;
  }

  @EventListener
  public void onDecisionSubmitted(DecisionSubmittedEvent event) {
    proofAnchoringService.createAndAnchor(event.decisionId());
  }
}
