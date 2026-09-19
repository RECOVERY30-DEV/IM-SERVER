package com.im.server.consultation.createconsultationreferral;

import com.im.server.comparison.api.ComparisonApi;
import com.im.server.comparison.api.ComparisonRunView;
import com.im.server.consultation.domain.ConsultationReferral;
import com.im.server.consultation.internal.ConsultationReferralRepository;
import com.im.server.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * S03 / S03-UNCERTAIN "상담원에게 문의" + "전송 동의" 오버레이. 실제 상담 예약·채팅 연동은 Non-goal이라 이 Handler는 핸드오프 사실만
 * 기록한다({@code externalConsultationRef}는 항상 null).
 */
@RestController
@Tag(name = "consultation", description = "상담 핸드오프 (경량)")
public class CreateConsultationReferralHandler {

  private final ComparisonApi comparisonApi;
  private final ConsultationReferralRepository consultationReferralRepository;

  public CreateConsultationReferralHandler(
      ComparisonApi comparisonApi, ConsultationReferralRepository consultationReferralRepository) {
    this.comparisonApi = comparisonApi;
    this.consultationReferralRepository = consultationReferralRepository;
  }

  @Operation(summary = "상담 핸드오프 생성", description = "상담원 문의 요청 사실과 전송 동의 여부를 기록한다.")
  @PostMapping("/api/comparisons/{comparisonId}/consultation-referrals")
  public ResponseEntity<ApiResponse<CreateConsultationReferralResponse>> handle(
      @PathVariable Long comparisonId, @RequestBody CreateConsultationReferralCommand command) {
    ComparisonRunView run = comparisonApi.getRun(comparisonId);

    ConsultationReferral referral =
        new ConsultationReferral(
            comparisonId, run.applicationId(), command.transferConsentGranted());
    consultationReferralRepository.save(referral);

    return ResponseEntity.ok(
        ApiResponse.success(
            new CreateConsultationReferralResponse(
                referral.getId(), referral.isTransferConsentGranted(), null)));
  }
}
