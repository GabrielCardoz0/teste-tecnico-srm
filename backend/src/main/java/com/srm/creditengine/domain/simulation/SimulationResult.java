package com.srm.creditengine.domain.simulation;

import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

public record SimulationResult(int termMonths, PricingResult pricing, ExchangeRate exchangeRate) {
}
