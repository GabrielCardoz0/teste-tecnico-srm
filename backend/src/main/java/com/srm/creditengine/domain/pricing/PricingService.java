package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

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
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
