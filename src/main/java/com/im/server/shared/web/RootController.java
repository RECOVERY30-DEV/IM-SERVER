package com.im.server.shared.web;

import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 루트 경로 안내. API 전용 백엔드라 {@code /} 로 들어오면 문서/헬스 링크를 알려준다. */
@RestController
@Tag(name = "Meta", description = "서비스 메타 정보")
public class RootController {

  @Operation(summary = "루트 안내", description = "API 문서·헬스체크 경로를 반환한다.")
  @GetMapping("/")
  public ResponseEntity<ApiResponse<Map<String, String>>> root() {
    return ResponseEntity.ok(
        ApiResponse.success(
            Map.of(
                "service", "IM API",
                "docs", "/swagger-ui/index.html",
                "openapi", "/v3/api-docs",
                "health", "/actuator/health")));
  }
}
