package com.im.server.decision.api;

/** decision 모듈 밖(proof 등)에서 Decision을 읽을 때 쓰는 유일한 통로. */
public interface DecisionApi {

  DecisionView getDecision(Long decisionId);
}
