package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

	static final BigDecimal BASE_RATE = new BigDecimal("0.01");

	private static final int MONEY_SCALE = 2;

	private final Map<ReceivableType, SpreadStrategy> strategies = new EnumMap<>(ReceivableType.class);

	public PricingService(List<SpreadStrategy> strategies) {
		for (SpreadStrategy strategy : strategies) {
			if (this.strategies.put(strategy.type(), strategy) != null) {
				throw new IllegalStateException("Duplicate spread strategy for " + strategy.type());
			}
		}
	}

	public PricingResult price(ReceivableType type, BigDecimal faceValue, int termMonths,
			PaymentCurrency paymentCurrency, BigDecimal usdBrlRate) {
		if (faceValue == null || faceValue.signum() <= 0) {
			throw new IllegalArgumentException("Face value must be positive");
		}
		if (termMonths <= 0) {
			throw new IllegalArgumentException("Term must be at least one month");
		}
		SpreadStrategy strategy = strategies.get(type);
		if (strategy == null) {
			throw new IllegalStateException("No spread strategy for " + type);
		}

		BigDecimal divisor = BigDecimal.ONE.add(BASE_RATE).add(strategy.spread()).pow(termMonths);
		BigDecimal presentValueBrl = faceValue.divide(divisor, MONEY_SCALE, RoundingMode.HALF_EVEN);
		BigDecimal discountBrl = faceValue.subtract(presentValueBrl);

		BigDecimal paymentAmount = switch (paymentCurrency) {
			case BRL -> presentValueBrl;
			case USD -> toUsd(presentValueBrl, usdBrlRate);
		};
		return new PricingResult(BASE_RATE, strategy.spread(), presentValueBrl, discountBrl, paymentCurrency,
				paymentAmount);
	}

	private static BigDecimal toUsd(BigDecimal amountBrl, BigDecimal usdBrlRate) {
		if (usdBrlRate == null || usdBrlRate.signum() <= 0) {
			throw new IllegalArgumentException("A positive USD/BRL rate is required to pay in USD");
		}
		return amountBrl.divide(usdBrlRate, MONEY_SCALE, RoundingMode.HALF_EVEN);
	}

}
