package com.im.server.condition.internal;

import com.im.server.condition.api.ConditionApi;
import com.im.server.condition.api.ConditionFields;
import com.im.server.condition.api.PostConditionSnapshotView;
import com.im.server.condition.api.PreConditionSnapshotView;
import com.im.server.condition.domain.PostConditionSnapshot;
import com.im.server.condition.domain.PreConditionSnapshot;
import com.im.server.shared.exception.BusinessException;
import com.im.server.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ConditionApiImpl implements ConditionApi {

  private final PreConditionSnapshotRepository preConditionSnapshotRepository;
  private final PostConditionSnapshotRepository postConditionSnapshotRepository;
  private final ObjectMapper objectMapper;

  public ConditionApiImpl(
      PreConditionSnapshotRepository preConditionSnapshotRepository,
      PostConditionSnapshotRepository postConditionSnapshotRepository,
      ObjectMapper objectMapper) {
    this.preConditionSnapshotRepository = preConditionSnapshotRepository;
    this.postConditionSnapshotRepository = postConditionSnapshotRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public PreConditionSnapshotView getPreSnapshot(Long preSnapshotId) {
    return toView(
        preConditionSnapshotRepository
            .findById(preSnapshotId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND)));
  }

  @Override
  public PostConditionSnapshotView getPostSnapshot(Long postSnapshotId) {
    return toView(
        postConditionSnapshotRepository
            .findById(postSnapshotId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND_FOR_POST)));
  }

  @Override
  public PreConditionSnapshotView getLatestPreSnapshot(String applicationId) {
    return toView(
        preConditionSnapshotRepository
            .findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND)));
  }

  @Override
  public PostConditionSnapshotView getLatestPostSnapshot(String applicationId) {
    return toView(
        postConditionSnapshotRepository
            .findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.CONDITION_PRE_SNAPSHOT_NOT_FOUND_FOR_POST)));
  }

  private PreConditionSnapshotView toView(PreConditionSnapshot snapshot) {
    return new PreConditionSnapshotView(
        snapshot.getId(),
        snapshot.getApplicationId(),
        snapshot.getCustomerId(),
        snapshot.getInquiredAt(),
        snapshot.getExpiresAt(),
        snapshot.getPayloadHash(),
        readFields(snapshot.getSnapshotPayload()));
  }

  private PostConditionSnapshotView toView(PostConditionSnapshot snapshot) {
    return new PostConditionSnapshotView(
        snapshot.getId(),
        snapshot.getApplicationId(),
        snapshot.getPreSnapshotId(),
        snapshot.getContractDocumentId(),
        snapshot.getContractDocumentHash(),
        snapshot.getDecidedAt(),
        snapshot.getPayloadHash(),
        readFields(snapshot.getSnapshotPayload()));
  }

  private ConditionFields readFields(String json) {
    try {
      return objectMapper.readValue(json, ConditionFields.class);
    } catch (Exception e) {
      throw new IllegalStateException("저장된 조건 payload를 읽을 수 없습니다", e);
    }
  }
}
