package com.srm.creditengine.api.settlement;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.infrastructure.persistence.settlement.SettlementStatementRow;

public record SettlementStatementLine(
		Long settlementId,
		Instant settledAt,
		String assignorLegalName,
		String assignorCnpj,
		ReceivableType receivableType,
		BigDecimal faceValue,
		BigDecimal presentValueBrl,
		BigDecimal discountBrl,
		PaymentCurrency paymentCurrency,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal exchangeRate,
		BigDecimal paymentAmount) {

	static SettlementStatementLine from(SettlementStatementRow row) {
		return new SettlementStatementLine(row.settlementId(), row.settledAt(), row.assignorLegalName(),
				row.assignorCnpj(), row.receivableType(), row.faceValue(), row.presentValueBrl(), row.discountBrl(),
				row.paymentCurrency(), row.exchangeRate(), row.paymentAmount());
	}

}
