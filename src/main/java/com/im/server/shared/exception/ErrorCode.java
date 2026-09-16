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
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다");

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
