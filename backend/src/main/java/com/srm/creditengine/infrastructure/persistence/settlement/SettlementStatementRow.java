package com.srm.creditengine.infrastructure.persistence.settlement;

import java.math.BigDecimal;
import java.time.Instant;

import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.ReceivableType;

public record SettlementStatementRow(
		Long settlementId,
		Instant settledAt,
		String assignorLegalName,
		String assignorCnpj,
		ReceivableType receivableType,
		BigDecimal faceValue,
		BigDecimal presentValueBrl,
		BigDecimal discountBrl,
		PaymentCurrency paymentCurrency,
		BigDecimal exchangeRate,
		BigDecimal paymentAmount) {
}
