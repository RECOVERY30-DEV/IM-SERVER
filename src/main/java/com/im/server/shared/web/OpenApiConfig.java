package com.im.server.shared.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 클라이언트 연동용 API 문서(Swagger UI: /swagger-ui.html, OpenAPI: /v3/api-docs) 설정. */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openApi() {
    // 상대 경로 서버 하나만 노출한다. "Try it out"이 Swagger UI 문서와 항상 같은 origin(scheme+host)으로
    // 호출하게 되어, 리버스 프록시 뒤에서 http/https 가 뒤섞여 CORS·mixed-content 로 막히는 문제를 없앤다.
    return new OpenAPI()
        .servers(List.of(new Server().url("/").description("current host")))
        .info(new Info().title("IM API").description("IM 백엔드 API 문서").version("v1"));
  }
}
