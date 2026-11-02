package com.srm.creditengine.infrastructure.persistence.settlement;

import java.time.Instant;

import com.srm.creditengine.domain.pricing.PaymentCurrency;

public record SettlementStatementFilter(
		Instant settledFromInclusive,
		Instant settledToExclusive,
		Long assignorId,
		PaymentCurrency paymentCurrency) {
}
