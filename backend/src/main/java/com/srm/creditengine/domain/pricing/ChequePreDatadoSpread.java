package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class ChequePreDatadoSpread implements SpreadStrategy {

	private static final BigDecimal SPREAD = new BigDecimal("0.025");

	@Override
	public ReceivableType type() {
		return ReceivableType.CHEQUE_PRE_DATADO;
	}

	@Override
	public BigDecimal spread() {
		return SPREAD;
	}

}
