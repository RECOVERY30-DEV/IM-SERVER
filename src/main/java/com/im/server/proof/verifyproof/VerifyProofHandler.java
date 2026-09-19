package com.im.server.proof.verifyproof;

import com.im.server.proof.api.BlockchainAdapter;
import com.im.server.proof.api.LedgerRecordView;
import com.im.server.proof.domain.ProofRecord;
import com.im.server.proof.domain.ProofVerification;
import com.im.server.proof.domain.VerifyResult;
import com.im.server.proof.internal.CanonicalProofPayloadFactory;
import com.im.server.proof.internal.ProofRecordRepository;
import com.im.server.proof.internal.ProofVerificationRepository;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** implementation-spec.md 13.7 verify(proofId) 그대로. */
@RestController
@Tag(name = "proof", description = "무결성 증빙 (Blockchain Adapter)")
public class VerifyProofHandler {

  private final ProofRecordRepository proofRecordRepository;
  private final ProofVerificationRepository proofVerificationRepository;
  private final BlockchainAdapter blockchainAdapter;

  public VerifyProofHandler(
      ProofRecordRepository proofRecordRepository,
      ProofVerificationRepository proofVerificationRepository,
      BlockchainAdapter blockchainAdapter) {
    this.proofRecordRepository = proofRecordRepository;
    this.proofVerificationRepository = proofVerificationRepository;
    this.blockchainAdapter = blockchainAdapter;
  }

  @Operation(
      summary = "Proof 재검증",
      description = "Off-chain Payload로 Hash를 재계산하고 원장값과 대조한다. 검증 실패도 200으로 응답한다.")
  @PostMapping("/api/proof/{proofId}:verify")
  public ResponseEntity<ApiResponse<VerifyProofResponse>> handle(@PathVariable String proofId) {
    ProofRecord record =
        proofRecordRepository
            .findByProofId(proofId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROOF_NOT_FOUND));

    String recomputedHash = CanonicalProofPayloadFactory.hashOf(record);
    Optional<LedgerRecordView> ledgerRecord = blockchainAdapter.getRecord(record.getRecordIdHash());

    VerifyProofResponse response;
    VerifyResult result;
    if (ledgerRecord.isEmpty()) {
      result = VerifyResult.NOT_ANCHORED;
      response = new VerifyProofResponse(false, result, recomputedHash, null, null);
    } else if (!ledgerRecord.get().payloadHash().equals(recomputedHash)) {
      result = VerifyResult.HASH_MISMATCH;
      response =
          new VerifyProofResponse(false, result, recomputedHash, record.getLedgerReference(), null);
    } else {
      result = VerifyResult.VERIFIED;
      response =
          new VerifyProofResponse(
              true,
              result,
              recomputedHash,
              record.getLedgerReference(),
              ledgerRecord.get().recordedAt());
    }

    proofVerificationRepository.save(
        new ProofVerification(
            record.getId(),
            "SYSTEM",
            recomputedHash,
            result,
            ledgerRecord.map(LedgerRecordView::recordedAt).orElse(null)));

    return ResponseEntity.ok(ApiResponse.success(response));
  }
}
