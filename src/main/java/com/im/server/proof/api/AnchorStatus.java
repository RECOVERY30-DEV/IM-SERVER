package com.im.server.proof.api;

/** proof_records.anchor_status — implementation-spec.md 13.6 제출·재시도 상태. */
public enum AnchorStatus {
  PENDING,
  SUBMITTED,
  CONFIRMED,
  FAILED
}
