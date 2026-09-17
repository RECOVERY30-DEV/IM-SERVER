package com.im.server.shared.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * implementation-spec.md 13.3의 Canonicalization 규칙(UTF-8, ISO-8601, 속성 정렬)을 만족하는 최소 구현. RFC
 * 8785(JCS) 라이브러리 자체는 쓰지 않지만 "정렬된 속성 + 고정 인코딩"이라는 동등한 규칙을 적용해 같은 입력이면 항상 같은 문자열·Hash가 나오게 한다.
 *
 * <p>Spring Boot 4는 Jackson 3(groupId {@code tools.jackson.core})을 기본으로 쓴다 — {@code
 * com.fasterxml.jackson.databind.ObjectMapper}가 아니라 이 패키지의 타입을 써야 Spring이 자동구성한 Bean과 동일한 계열이다.
 */
public final class CanonicalJson {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  private CanonicalJson() {}

  public static String canonicalize(Object value) {
    return MAPPER.writeValueAsString(value);
  }

  public static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
    }
  }

  public static String hashOf(Object value) {
    return sha256Hex(canonicalize(value));
  }
}
