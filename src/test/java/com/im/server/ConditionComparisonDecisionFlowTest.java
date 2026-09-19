package com.im.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * S01(사전조건 저장) → V2 저장(자동 비교) → S02/S03(요약) → S04(항목 상세·확인) → S05(Review Gate·결정)까지
 * condition·comparison·decision 세 모듈을 가로지르는 핵심 흐름 전체를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConditionComparisonDecisionFlowTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void V1_저장후_V2를_저장하면_비교가_자동실행되고_필수확인을_마쳐야_약정을_진행할_수_있다() throws Exception {
    String applicationId = "app_" + UUID.randomUUID();

    // S01: 사전조건(V1) 저장 — 급여이체 + iM뱅크 주거래 우대금리가 반영된 연 5.20%
    String v1Body =
        """
        {
          "customerId": "cust_0001",
          "productCode": "PL-CREDIT-01",
          "productVersion": "2026-09-01",
          "inquiredAt": "2026-09-11T09:00:00Z",
          "expiresAt": "2026-09-12T18:00:00Z",
          "conditions": {
            "loanAmount": 50000000,
            "baseRatePercent": 3.50,
            "spreadRatePercent": 2.00,
            "preferentialRatePercent": 0.40,
            "finalRatePercent": 5.20,
            "termMonths": 36,
            "repaymentMethod": "EQUAL_INSTALLMENT",
            "preferentialConditions": [
              {"code": "SALARY_TRANSFER", "label": "급여이체", "ratePercentOff": 0.30},
              {"code": "IM_BANK_PRIMARY", "label": "iM뱅크 주거래", "ratePercentOff": 0.10}
            ],
            "stampTax": 50000,
            "fixedUpfrontFees": 20000,
            "delinquencyRatePercent": 15.0
          }
        }
        """;

    String v1Response =
        mockMvc
            .perform(
                post("/api/applications/{applicationId}/pre-conditions", applicationId)
                    .header("Idempotency-Key", "idem-" + applicationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(v1Body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.preSnapshotId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long preSnapshotId = readData(v1Response).path("preSnapshotId").asLong();

    // 최종 심사 완료: V2 저장 — 급여이체 실적 미확인으로 우대금리 0.30%p 미반영 → 최종금리 5.50%
    String v2Body =
        """
        {
          "reviewVersion": "review-2026-09-12",
          "contractDocumentId": "contract_0001",
          "contractDocumentHash": "%s",
          "decidedAt": "2026-09-12T09:00:00Z",
          "conditions": {
            "loanAmount": 50000000,
            "baseRatePercent": 3.50,
            "spreadRatePercent": 2.00,
            "preferentialRatePercent": 0.10,
            "finalRatePercent": 5.50,
            "termMonths": 36,
            "repaymentMethod": "EQUAL_INSTALLMENT",
            "preferentialConditions": [
              {"code": "IM_BANK_PRIMARY", "label": "iM뱅크 주거래", "ratePercentOff": 0.10}
            ],
            "stampTax": 50000,
            "fixedUpfrontFees": 20000,
            "delinquencyRatePercent": 15.0
          }
        }
        """
            .formatted("a".repeat(64));

    String v2Response =
        mockMvc
            .perform(
                post("/api/applications/{applicationId}/post-conditions", applicationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(v2Body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.preSnapshotId").value(preSnapshotId))
            .andExpect(jsonPath("$.data.comparisonId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long comparisonId = readData(v2Response).path("comparisonId").asLong();

    // S02/S03: 비교 결과는 즉시 완료 상태이고, 최종금리 상승으로 CHECK_REQUIRED
    mockMvc
        .perform(get("/api/comparisons/{comparisonId}", comparisonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.overallStatus").value("CHECK_REQUIRED"))
        .andExpect(jsonPath("$.data.progress.percent").value(100));

    String summaryResponse =
        mockMvc
            .perform(get("/api/comparisons/{comparisonId}/summary", comparisonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.overallStatus").value("CHECK_REQUIRED"))
            .andExpect(jsonPath("$.data.impact.monthlyPaymentDelta").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode headline = readData(summaryResponse).path("headlineItems");
    Assertions.assertTrue(headline.size() >= 2, "금리·우대조건 변경이 헤드라인에 잡혀야 한다");

    // S04: 대출금리(FINAL_RATE) 항목 상세 — WORSE, +0.30%p
    long finalRateItemId = findItemId(comparisonId, "FINAL_RATE");
    mockMvc
        .perform(get("/api/comparisons/items/{itemId}", finalRateItemId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.itemStatus").value("WORSE"))
        .andExpect(jsonPath("$.data.deltaLabel").value("+0.30%p"))
        .andExpect(jsonPath("$.data.requiresReview").value(true));

    // S05 진입 전: 아직 확인 전이라 PROCEED 불가
    mockMvc
        .perform(get("/api/comparisons/{comparisonId}/review-gate", comparisonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.allReviewed").value(false));

    mockMvc
        .perform(
            post("/api/comparisons/{comparisonId}/decisions", comparisonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"decisionType\":\"PROCEED\",\"signatureSessionId\":null,\"bypassedUncertainItems\":false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("DECISION_400_1"));

    // 필수 확인 항목을 모두 확인
    for (JsonNode item : readItems(comparisonId, "WORSE,STRUCTURAL_CHANGE,UNKNOWN")) {
      mockMvc
          .perform(post("/api/comparisons/items/{itemId}:review", item.path("itemId").asLong()))
          .andExpect(status().isOk());
    }

    mockMvc
        .perform(get("/api/comparisons/{comparisonId}/review-gate", comparisonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.allReviewed").value(true));

    // 확인 없이 서명 세션 없이 결정하면 거부 → 서명 세션 발급
    String sessionResponse =
        mockMvc
            .perform(post("/api/comparisons/{comparisonId}/signature-sessions", comparisonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String sessionId = readData(sessionResponse).path("sessionId").asString();

    // S05: 이제 PROCEED 가능
    String decisionResponse =
        mockMvc
            .perform(
                post("/api/comparisons/{comparisonId}/decisions", comparisonId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"decisionType":"PROCEED","signatureSessionId":"%s","bypassedUncertainItems":false}
                        """
                            .formatted(sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.decisionType").value("PROCEED"))
            .andExpect(jsonPath("$.data.signedAt").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long decisionId = readData(decisionResponse).path("decisionId").asLong();

    // 같은 서명 세션은 재사용할 수 없다
    mockMvc
        .perform(
            post("/api/comparisons/{comparisonId}/decisions", comparisonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"decisionType":"PROCEED","signatureSessionId":"%s","bypassedUncertainItems":false}
                    """
                        .formatted(sessionId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("DECISION_409_3"));

    // S06: PROCEED 저장 즉시 Proof가 생성·anchoring(Mock)까지 동기로 끝나 CONFIRMED여야 한다
    String proofStatusResponse =
        mockMvc
            .perform(get("/api/decisions/{decisionId}/proof", decisionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.anchorStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.recordSummary.allReviewed").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode proofStatusData = readData(proofStatusResponse);
    Assertions.assertTrue(
        proofStatusData.path("recordSummary").path("changedItemsCount").asInt() >= 2);
    String proofId = proofStatusData.path("proofId").asString();

    // "기록 자세히 보기" — Canonical Payload 필드
    mockMvc
        .perform(get("/api/proof/{proofId}", proofId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anchorStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.data.payloadHash").exists())
        .andExpect(jsonPath("$.data.ledgerReference").exists());

    // 재검증 — Off-chain Payload를 재해싱해서 원장값과 대조하면 일치해야 한다
    mockMvc
        .perform(post("/api/proof/{proofId}:verify", proofId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.verified").value(true))
        .andExpect(jsonPath("$.data.reason").value("VERIFIED"));

    // 이미 동일 정책 Version으로 비교가 존재하므로 재시도는 거부된다
    mockMvc
        .perform(post("/api/applications/{applicationId}/comparisons:retry", applicationId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("COMPARISON_409_2"));

    // 상담 핸드오프
    mockMvc
        .perform(
            post("/api/comparisons/{comparisonId}/consultation-referrals", comparisonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transferConsentGranted\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.transferConsentGranted").value(true))
        .andExpect(jsonPath("$.data.referralId").exists());
  }

  @Test
  void 사전조건_없이_최종조건을_저장하면_404와_에러코드를_반환한다() throws Exception {
    String body =
        """
        {
          "reviewVersion": "review-2026-09-12",
          "contractDocumentId": "contract_orphan",
          "contractDocumentHash": "%s",
          "decidedAt": "2026-09-12T09:00:00Z",
          "conditions": {
            "loanAmount": 10000000, "baseRatePercent": 3.0, "spreadRatePercent": 2.0,
            "preferentialRatePercent": 0.0, "finalRatePercent": 5.0, "termMonths": 12,
            "repaymentMethod": "EQUAL_INSTALLMENT", "preferentialConditions": [],
            "stampTax": 0, "fixedUpfrontFees": 0, "delinquencyRatePercent": 15.0
          }
        }
        """
            .formatted("b".repeat(64));

    mockMvc
        .perform(
            post("/api/applications/{applicationId}/post-conditions", "app_never_had_v1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("CONDITION_404_2"));
  }

  @Test
  void 동일_IdempotencyKey로_다른_내용을_요청하면_409를_반환한다() throws Exception {
    String applicationId = "app_" + UUID.randomUUID();
    String key = "idem-conflict-" + applicationId;

    mockMvc.perform(
        post("/api/applications/{applicationId}/pre-conditions", applicationId)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(preConditionBody(50_000_000)));

    mockMvc
        .perform(
            post("/api/applications/{applicationId}/pre-conditions", applicationId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preConditionBody(99_999_999)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONDITION_409_1"));
  }

  private String preConditionBody(long loanAmount) {
    return """
        {
          "customerId": "cust_0002",
          "productCode": "PL-CREDIT-01",
          "productVersion": "2026-09-01",
          "inquiredAt": "2026-09-11T09:00:00Z",
          "expiresAt": "2026-09-12T18:00:00Z",
          "conditions": {
            "loanAmount": %d, "baseRatePercent": 3.50, "spreadRatePercent": 2.00,
            "preferentialRatePercent": 0.40, "finalRatePercent": 5.20, "termMonths": 36,
            "repaymentMethod": "EQUAL_INSTALLMENT",
            "preferentialConditions": [],
            "stampTax": 50000, "fixedUpfrontFees": 20000, "delinquencyRatePercent": 15.0
          }
        }
        """
        .formatted(loanAmount);
  }

  private JsonNode readData(String response) {
    return objectMapper.readTree(response).path("data");
  }

  private List<JsonNode> readItems(long comparisonId, String status) throws Exception {
    String response =
        mockMvc
            .perform(
                get("/api/comparisons/{comparisonId}/items", comparisonId).param("status", status))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode data = readData(response);
    return data.valueStream().toList();
  }

  private long findItemId(long comparisonId, String fieldCode) throws Exception {
    return readItems(comparisonId, "SAME,BETTER,WORSE,STRUCTURAL_CHANGE,UNKNOWN").stream()
        .filter(item -> fieldCode.equals(item.path("fieldCode").asString()))
        .findFirst()
        .orElseThrow()
        .path("itemId")
        .asLong();
  }
}
