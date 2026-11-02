package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

public record PricingResult(
		BigDecimal baseRate,
		BigDecimal spread,
		BigDecimal presentValueBrl,
		BigDecimal discountBrl,
		PaymentCurrency paymentCurrency,
		BigDecimal paymentAmount) {
}
