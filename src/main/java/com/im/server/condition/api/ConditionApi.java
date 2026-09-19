package com.im.server.condition.api;

/** condition 모듈 밖(comparison 등)에서 Snapshot을 읽을 때 쓰는 유일한 통로. */
public interface ConditionApi {

  PreConditionSnapshotView getPreSnapshot(Long preSnapshotId);

  PostConditionSnapshotView getPostSnapshot(Long postSnapshotId);
}
