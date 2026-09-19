package com.im.server.proof.api;

import java.util.Optional;

/**
 * implementation-spec.md 13.4 Blockchain Adapter Contract의 Java 표현. 네트워크·SDK는 지정하지 않는다 — 구현체(예:
 * {@code proof.internal.MockBlockchainAdapter})만 교체하면 실제 체인으로 옮길 수 있다.
 */
public interface BlockchainAdapter {

  AnchorRecordResult anchorRecord(
      String recordIdHash, String payloadHash, String schemaVersion, String supersedesRecordIdHash);

  Optional<LedgerRecordView> getRecord(String recordIdHash);

  ConfirmationResult getConfirmation(String ledgerReference);
}
