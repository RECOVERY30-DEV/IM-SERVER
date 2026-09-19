package com.im.server.proof.api;

import java.time.Instant;

public record AnchorRecordResult(String ledgerReference, Instant submittedAt) {}
