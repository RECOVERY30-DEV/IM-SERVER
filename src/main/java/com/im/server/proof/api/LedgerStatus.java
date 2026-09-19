package com.im.server.proof.api;

/** implementation-spec.md 13.4 getConfirmation()이 반환하는 원장 확인 상태. */
public enum LedgerStatus {
  PENDING,
  CONFIRMED,
  FAILED
}
