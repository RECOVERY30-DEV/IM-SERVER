package com.im.server.shared.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 실패 응답에 담기는 에러 정보. */
public record ApiError(
    @Schema(description = "에러 코드", example = "COMMON_400") String code,
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다") String message) {}
