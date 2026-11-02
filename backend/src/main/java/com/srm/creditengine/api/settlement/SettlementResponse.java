package com.srm.creditengine.api.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.infrastructure.persistence.settlement.Settlement;

public record SettlementResponse(
		Long id,
		UUID idempotencyKey,
		Long receivableId,
		ReceivableType receivableType,
		BigDecimal faceValue,
		int termMonths,
		BigDecimal baseRate,
		BigDecimal spread,
		BigDecimal presentValueBrl,
		BigDecimal discountBrl,
		PaymentCurrency paymentCurrency,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long exchangeRateId,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal exchangeRate,
		BigDecimal paymentAmount,
		Instant settledAt,
		Instant createdAt) {

	static SettlementResponse from(Settlement settlement) {
		return new SettlementResponse(settlement.getId(), settlement.getIdempotencyKey(),
				settlement.getReceivableId(), settlement.getReceivableType(), settlement.getFaceValue(),
				settlement.getTermMonths(), settlement.getBaseRate(), settlement.getSpread(),
				settlement.getPresentValueBrl(), settlement.getDiscountBrl(), settlement.getPaymentCurrency(),
				settlement.getExchangeRateId(), settlement.getExchangeRate(), settlement.getPaymentAmount(),
				settlement.getSettledAt(), settlement.getCreatedAt());
	}

}
