package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class DuplicataMercantilSpread implements SpreadStrategy {

	private static final BigDecimal SPREAD = new BigDecimal("0.015");

	@Override
	public ReceivableType type() {
		return ReceivableType.DUPLICATA_MERCANTIL;
	}

	@Override
	public BigDecimal spread() {
		return SPREAD;
	}

}
