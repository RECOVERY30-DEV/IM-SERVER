package com.im.server.shared.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 모든 API 응답을 감싸는 공통 포맷. success면 data, 실패면 error만 채워진다. */
public record ApiResponse<T>(
    @Schema(description = "요청 성공 여부") boolean success,
    @Schema(description = "성공 시 응답 데이터") T data,
    @Schema(description = "실패 시 에러 정보") ApiError error) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static ApiResponse<Void> error(ApiError error) {
    return new ApiResponse<>(false, null, error);
  }
}
