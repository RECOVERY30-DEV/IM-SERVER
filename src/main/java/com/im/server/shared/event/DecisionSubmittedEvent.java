package com.im.server.shared.event;

/** PROCEED 결정이 저장되면 발행된다. proof 모듈이 이 이벤트를 구독해서 증빙(Proof)을 만들고 anchoring한다. */
public record DecisionSubmittedEvent(Long decisionId, Long comparisonRunId) {}
