package com.im.server.proof.internal;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.condition.api.ConditionApi;
import com.im.server.condition.api.PostConditionSnapshotView;
import com.im.server.condition.api.PreConditionSnapshotView;
import com.im.server.decision.api.DecisionApi;
import com.im.server.decision.api.DecisionView;
import com.im.server.proof.api.AnchorRecordResult;
import com.im.server.proof.api.BlockchainAdapter;
import com.im.server.proof.api.ConfirmationResult;
import com.im.server.proof.api.LedgerStatus;
import com.im.server.proof.domain.ProofRecord;
import com.im.server.shared.util.CanonicalJson;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * FR13 — Decision(PROCEED) 저장 직후 Proof를 만들고 즉시 anchoring한다(동기, MockBlockchainAdapter 기준). 진짜 체인으로
 * 교체되면 anchoring은 비동기가 되고 13.6 재시도 스케줄이 필요해진다 — 지금은 목 어댑터가 항상 즉시 성공하므로 재시도 로직은 아직 실질적인 의미가 없다.
 */
@Service
public class ProofAnchoringService {

  private static final String SCHEMA_VERSION = "1.0";

  private final DecisionApi decisionApi;
  private final ComparisonApi comparisonApi;
  private final ConditionApi conditionApi;
  private final BlockchainAdapter blockchainAdapter;
  private final ProofRecordRepository proofRecordRepository;
  private final String salt;
  private final String issuerId;

  public ProofAnchoringService(
      DecisionApi decisionApi,
      ComparisonApi comparisonApi,
      ConditionApi conditionApi,
      BlockchainAdapter blockchainAdapter,
      ProofRecordRepository proofRecordRepository,
      @Value("${im.proof.salt}") String salt,
      @Value("${im.proof.issuer-id}") String issuerId) {
    this.decisionApi = decisionApi;
    this.comparisonApi = comparisonApi;
    this.conditionApi = conditionApi;
    this.blockchainAdapter = blockchainAdapter;
    this.proofRecordRepository = proofRecordRepository;
    this.salt = salt;
    this.issuerId = issuerId;
  }

  public Long createAndAnchor(Long decisionId) {
    var existing = proofRecordRepository.findByDecisionId(decisionId);
    if (existing.isPresent()) {
      return existing.get().getId();
    }

    DecisionView decision = decisionApi.getDecision(decisionId);
    ComparisonRunView run = comparisonApi.getRun(decision.comparisonRunId());
    PreConditionSnapshotView preSnapshot = conditionApi.getPreSnapshot(run.preSnapshotId());
    PostConditionSnapshotView postSnapshot = conditionApi.getPostSnapshot(run.postSnapshotId());

    String proofId = "proof_" + UUID.randomUUID();
    String recordIdHash = CanonicalJson.sha256Hex(salt + ":" + proofId);
    String checkIdHash = CanonicalJson.sha256Hex(salt + ":" + run.id());
    String applicationIdHash = CanonicalJson.sha256Hex(salt + ":" + run.applicationId());
    String decisionHash = hashDecision(decision);

    Instant createdAt = Instant.now();
    String payloadHash =
        CanonicalProofPayloadFactory.hash(
            SCHEMA_VERSION,
            proofId,
            checkIdHash,
            applicationIdHash,
            preSnapshot.payloadHash(),
            postSnapshot.payloadHash(),
            run.comparisonHash(),
            decisionHash,
            issuerId,
            run.promptVersion(),
            run.ruleVersion(),
            run.calculationVersion(),
            createdAt,
            null);

    ProofRecord record =
        new ProofRecord(
            decisionId,
            proofId,
            recordIdHash,
            SCHEMA_VERSION,
            checkIdHash,
            applicationIdHash,
            preSnapshot.payloadHash(),
            postSnapshot.payloadHash(),
            run.comparisonHash(),
            decisionHash,
            issuerId,
            run.promptVersion(),
            run.ruleVersion(),
            run.calculationVersion(),
            payloadHash,
            createdAt);
    proofRecordRepository.save(record);

    anchor(record, recordIdHash, payloadHash);
    proofRecordRepository.save(record);
    return record.getId();
  }

  private void anchor(ProofRecord record, String recordIdHash, String payloadHash) {
    try {
      AnchorRecordResult anchorResult =
          blockchainAdapter.anchorRecord(recordIdHash, payloadHash, SCHEMA_VERSION, null);
      record.markSubmitted(anchorResult.ledgerReference());

      ConfirmationResult confirmation =
          blockchainAdapter.getConfirmation(anchorResult.ledgerReference());
      if (confirmation.status() == LedgerStatus.CONFIRMED) {
        record.markConfirmed(confirmation.confirmationCount());
      } else {
        record.markFailed();
      }
    } catch (Exception e) {
      record.markFailed();
    }
  }

  private String hashDecision(DecisionView decision) {
    return CanonicalJson.hashOf(
        new Object[] {
          decision.id(),
          decision.decisionType(),
          decision.signedAt(),
          decision.postSnapshotIdAtDecision(),
          decision.contractDocumentHashAtDecision(),
          decision.allItemsConfirmed()
        });
  }
}
