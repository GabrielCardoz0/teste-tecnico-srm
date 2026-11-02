package com.srm.creditengine.api.pricing;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.simulation.SimulationResult;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

public record SimulationResponse(
		int termMonths,
		BigDecimal baseRate,
		BigDecimal spread,
		BigDecimal presentValueBrl,
		BigDecimal discountBrl,
		PaymentCurrency paymentCurrency,
		@JsonInclude(JsonInclude.Include.NON_NULL) ExchangeRateUsed exchangeRate,
		@JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal amountUsd) {

	public record ExchangeRateUsed(Long id, BigDecimal rate, Instant validFrom) {
	}

	static SimulationResponse from(SimulationResult result) {
		PricingResult pricing = result.pricing();
		ExchangeRate rate = result.exchangeRate();
		boolean usd = pricing.paymentCurrency() == PaymentCurrency.USD;
		return new SimulationResponse(
				result.termMonths(),
				pricing.baseRate(),
				pricing.spread(),
				pricing.presentValueBrl(),
				pricing.discountBrl(),
				pricing.paymentCurrency(),
				usd ? new ExchangeRateUsed(rate.getId(), rate.getRate(), rate.getValidFrom()) : null,
				usd ? pricing.paymentAmount() : null);
	}

}
