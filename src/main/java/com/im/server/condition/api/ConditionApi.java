package com.im.server.condition.api;

/** condition 모듈 밖(comparison 등)에서 Snapshot을 읽을 때 쓰는 유일한 통로. */
public interface ConditionApi {

  PreConditionSnapshotView getPreSnapshot(Long preSnapshotId);

  PostConditionSnapshotView getPostSnapshot(Long postSnapshotId);

  /** 신청건에 대해 가장 최근 저장된 V1. 재비교(:retry) 등에 쓴다. */
  PreConditionSnapshotView getLatestPreSnapshot(String applicationId);

  /** 신청건에 대해 가장 최근 저장된 V2. 재비교(:retry), Decision의 계약 Version 재검증에 쓴다. */
  PostConditionSnapshotView getLatestPostSnapshot(String applicationId);
}
