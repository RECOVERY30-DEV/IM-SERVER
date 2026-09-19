package com.im.server.proof.api;

public record ConfirmationResult(LedgerStatus status, int confirmationCount) {}
