package com.im.server.proof.internal;

import com.im.server.proof.domain.ProofRecord;
import com.im.server.shared.util.CanonicalJson;
import java.time.Instant;

/**
 * {@link CanonicalProofPayload}를 만드는 유일한 통로. 생성 시점(ProofAnchoringService)과 검증 시점
 * (VerifyProofHandler)이 이 클래스를 통해 정확히 같은 필드 순서로 payload를 구성해야 payloadHash가 재현 가능하다 — 따로 만들면 필드 하나만
 * 어긋나도 항상 HASH_MISMATCH가 난다.
 */
public final class CanonicalProofPayloadFactory {

  private CanonicalProofPayloadFactory() {}

  public static String hash(
      String schemaVersion,
      String proofId,
      String checkIdHash,
      String applicationIdHash,
      String v1SnapshotHash,
      String v2SnapshotHash,
      String comparisonResultHash,
      String decisionHash,
      String issuerId,
      String promptVersion,
      String ruleVersion,
      String calculationVersion,
      Instant createdAt,
      String supersedesRecordId) {
    CanonicalProofPayload payload =
        new CanonicalProofPayload(
            schemaVersion,
            proofId,
            checkIdHash,
            applicationIdHash,
            v1SnapshotHash,
            v2SnapshotHash,
            comparisonResultHash,
            decisionHash,
            issuerId,
            promptVersion,
            ruleVersion,
            calculationVersion,
            createdAt,
            supersedesRecordId);
    return CanonicalJson.hashOf(payload);
  }

  public static String hashOf(ProofRecord record) {
    return hash(
        record.getSchemaVersion(),
        record.getProofId(),
        record.getCheckIdHash(),
        record.getApplicationIdHash(),
        record.getV1SnapshotHash(),
        record.getV2SnapshotHash(),
        record.getComparisonResultHash(),
        record.getDecisionHash(),
        record.getIssuerId(),
        record.getPromptVersion(),
        record.getRuleVersion(),
        record.getCalculationVersion(),
        record.getCreatedAt(),
        record.getSupersedesRecordIdHash());
  }
}
