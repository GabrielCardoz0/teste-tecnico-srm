package com.srm.creditengine.domain.simulation;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.domain.exchangerate.ExchangeRateService;
import com.srm.creditengine.domain.exchangerate.NoCurrentExchangeRateException;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

@Service
public class SimulationService {

	private final TermCalculator termCalculator;
	private final PricingService pricingService;
	private final ExchangeRateService exchangeRateService;

	public SimulationService(TermCalculator termCalculator, PricingService pricingService,
			ExchangeRateService exchangeRateService) {
		this.termCalculator = termCalculator;
		this.pricingService = pricingService;
		this.exchangeRateService = exchangeRateService;
	}

	@Transactional(readOnly = true)
	public SimulationResult simulate(ReceivableType type, BigDecimal faceValue, LocalDate dueDate,
			PaymentCurrency paymentCurrency) {
		int termMonths = termCalculator.termInMonths(dueDate);

		ExchangeRate exchangeRate = paymentCurrency == PaymentCurrency.USD
				? exchangeRateService.findCurrent().orElseThrow(NoCurrentExchangeRateException::new)
				: null;

		PricingResult pricing = pricingService.price(type, faceValue, termMonths, paymentCurrency,
				exchangeRate != null ? exchangeRate.getRate() : null);
		return new SimulationResult(termMonths, pricing, exchangeRate);
	}

}
