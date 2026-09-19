package com.im.server.consultation.internal;

import com.im.server.consultation.domain.ConsultationReferral;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationReferralRepository extends JpaRepository<ConsultationReferral, Long> {}
