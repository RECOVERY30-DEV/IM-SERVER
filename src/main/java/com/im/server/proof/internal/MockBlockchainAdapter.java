package com.im.server.proof.internal;

import com.im.server.proof.api.AnchorRecordResult;
import com.im.server.proof.api.BlockchainAdapter;
import com.im.server.proof.api.ConfirmationResult;
import com.im.server.proof.api.LedgerRecordView;
import com.im.server.proof.api.LedgerStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * PRD 12.1 — 은행 단독 MVP 기준 블록체인 필요도 6/10. 실제 네트워크 대신 DB 테이블(`proof_mock_ledger`)을 "원장"으로 흉내 낸다. 진짜
 * 체인은 제출→컨펌까지 지연·실패가 있지만 이 목 구현은 즉시 확정(CONFIRMED)한다 — {@code docs/db-design.md} 6장에 명시된 대로, 실제 원장이
 * 정해지면 이 클래스만 교체하면 된다.
 */
@Service
public class MockBlockchainAdapter implements BlockchainAdapter {

  private final MockLedgerRepository mockLedgerRepository;

  public MockBlockchainAdapter(MockLedgerRepository mockLedgerRepository) {
    this.mockLedgerRepository = mockLedgerRepository;
  }

  @Override
  public AnchorRecordResult anchorRecord(
      String recordIdHash,
      String payloadHash,
      String schemaVersion,
      String supersedesRecordIdHash) {
    // 13.5 — 동일 recordId 재등록은 기존 payloadHash가 같으면 Idempotent Success
    Optional<MockLedgerEntry> existing = mockLedgerRepository.findByRecordIdHash(recordIdHash);
    if (existing.isPresent()) {
      MockLedgerEntry entry = existing.get();
      if (!entry.getPayloadHash().equals(payloadHash)) {
        throw new IllegalStateException("동일 recordId에 다른 payloadHash로 재등록을 시도했습니다");
      }
      return new AnchorRecordResult(entry.getLedgerReference(), entry.getRecordedAt());
    }

    String ledgerReference = "mock-ledger:" + UUID.randomUUID();
    MockLedgerEntry entry =
        new MockLedgerEntry(
            recordIdHash,
            payloadHash,
            schemaVersion,
            "im-bank-demo",
            supersedesRecordIdHash,
            ledgerReference);
    mockLedgerRepository.save(entry);
    return new AnchorRecordResult(ledgerReference, entry.getRecordedAt());
  }

  @Override
  public Optional<LedgerRecordView> getRecord(String recordIdHash) {
    return mockLedgerRepository
        .findByRecordIdHash(recordIdHash)
        .map(
            entry ->
                new LedgerRecordView(
                    entry.getPayloadHash(),
                    entry.getSchemaVersion(),
                    entry.getIssuerId(),
                    entry.getRecordedAt(),
                    entry.getSupersedesRecordIdHash()));
  }

  @Override
  public ConfirmationResult getConfirmation(String ledgerReference) {
    return mockLedgerRepository
        .findByLedgerReference(ledgerReference)
        .map(entry -> new ConfirmationResult(LedgerStatus.CONFIRMED, entry.getConfirmationCount()))
        .orElse(new ConfirmationResult(LedgerStatus.FAILED, 0));
  }
}
