package com.im.server.proof.domain;

/** implementation-spec.md 13.7 verify()의 반환 사유. */
public enum VerifyResult {
  VERIFIED,
  NOT_ANCHORED,
  HASH_MISMATCH
}
