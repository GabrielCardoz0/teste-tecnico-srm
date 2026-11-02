package com.srm.creditengine.api.exchangerate;

import java.math.BigDecimal;
import java.time.Instant;

import com.srm.creditengine.domain.exchangerate.CurrencyCode;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

public record ExchangeRateResponse(
		Long id,
		CurrencyCode sourceCurrency,
		CurrencyCode targetCurrency,
		BigDecimal rate,
		Instant validFrom,
		Instant createdAt) {

	static ExchangeRateResponse from(ExchangeRate exchangeRate) {
		return new ExchangeRateResponse(exchangeRate.getId(), exchangeRate.getSourceCurrency(),
				exchangeRate.getTargetCurrency(), exchangeRate.getRate(), exchangeRate.getValidFrom(),
				exchangeRate.getCreatedAt());
	}

}
