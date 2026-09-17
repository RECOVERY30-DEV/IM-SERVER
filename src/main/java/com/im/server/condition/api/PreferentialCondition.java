package com.im.server.condition.api;

import java.math.BigDecimal;

/** PRD 10장 PREFERENTIAL_CONDITIONS의 항목 하나. */
public record PreferentialCondition(String code, String label, BigDecimal ratePercentOff) {}
