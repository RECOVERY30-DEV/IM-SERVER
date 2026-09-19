package com.im.server.decision.internal;

import com.im.server.decision.domain.SignatureSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignatureSessionRepository extends JpaRepository<SignatureSession, Long> {

  Optional<SignatureSession> findBySessionId(String sessionId);
}
