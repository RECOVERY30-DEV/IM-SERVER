package com.im.server.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 쓰는 에러 코드. 새 모듈이 에러를 추가할 땐 이 enum에 상수를 추가한다.
 *
 * <p>네이밍: {모듈}_{HTTP상태}_{순번} (예: MEMBER_400_2), 공통 에러는 COMMON_ 접두사.
 */
public enum ErrorCode {
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다"),
  MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "COMMON_400_1", "요청 본문을 읽을 수 없습니다"),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 경로를 찾을 수 없습니다"),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "허용되지 않은 HTTP 메서드입니다"),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다"),

  CONDITION_INVALID_FIELDS(HttpStatus.BAD_REQUEST, "CONDITION_400_1", "조건 값이 올바르지 않습니다"),
  CONDITION_PRE_SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONDITION_404_1", "유효한 사전조건 조회 이력이 없습니다"),
  CONDITION_PRE_SNAPSHOT_NOT_FOUND_FOR_POST(
      HttpStatus.NOT_FOUND, "CONDITION_404_2", "최종조건과 매칭되는 사전조건이 없습니다"),
  CONDITION_IDEMPOTENCY_KEY_CONFLICT(
      HttpStatus.CONFLICT, "CONDITION_409_1", "동일한 Idempotency-Key로 다른 내용의 요청이 이미 존재합니다"),
  CONDITION_POST_SNAPSHOT_DUPLICATE(
      HttpStatus.CONFLICT, "CONDITION_409_2", "동일한 계약서로 이미 최종조건이 등록되어 있습니다"),

  COMPARISON_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPARISON_404_1", "존재하지 않는 비교 결과입니다"),
  COMPARISON_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPARISON_404_2", "존재하지 않는 비교 항목입니다"),
  COMPARISON_ITEM_REVIEW_NOT_REQUIRED(
      HttpStatus.BAD_REQUEST, "COMPARISON_400_1", "확인이 필요하지 않은 항목입니다"),
  COMPARISON_RUN_NOT_COMPLETED(HttpStatus.CONFLICT, "COMPARISON_409_1", "아직 비교가 완료되지 않았습니다"),

  DECISION_REVIEW_GATE_NOT_CLEARED(
      HttpStatus.BAD_REQUEST, "DECISION_400_1", "필수 확인 항목을 모두 확인해야 진행할 수 있습니다"),
  DECISION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "DECISION_409_1", "이미 결정이 저장된 비교입니다");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
