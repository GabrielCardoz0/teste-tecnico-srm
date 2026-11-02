package com.srm.creditengine.api.exchangerate;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srm.creditengine.domain.exchangerate.CurrencyCode;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateExchangeRateRequest(
		@NotNull CurrencyCode sourceCurrency,
		@NotNull CurrencyCode targetCurrency,
		@NotNull @Positive @Digits(integer = 11, fraction = 8) BigDecimal rate,
		Instant validFrom) {

	@JsonIgnore
	@AssertTrue(message = "only USD to BRL exchange rates are supported")
	public boolean isUsdToBrl() {
		return sourceCurrency == null || targetCurrency == null
				|| (sourceCurrency == CurrencyCode.USD && targetCurrency == CurrencyCode.BRL);
	}

}
