package com.im.server.shared.exception;

import com.im.server.shared.response.ApiError;
import com.im.server.shared.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 컨트롤러(Handler)의 예외를 ApiResponse 형태로 통일해서 내려준다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    return toResponse(e.getErrorCode(), e.getMessage());
  }

  /** 매핑된 핸들러/정적 리소스가 없는 경로 (예: GET /). 500이 아니라 404로 내린다. */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
    return toResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage());
  }

  /** 잘못된/누락된 요청 본문, 파라미터 타입 불일치, 필수 파라미터 누락 → 400. */
  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
    return toResponse(
        ErrorCode.MALFORMED_REQUEST_BODY, ErrorCode.MALFORMED_REQUEST_BODY.getMessage());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("처리되지 않은 예외", e);
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    return toResponse(errorCode, errorCode.getMessage());
  }

  private static ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String message) {
    return ResponseEntity.status(errorCode.getStatus())
        .body(ApiResponse.error(new ApiError(errorCode.getCode(), message)));
  }
}
