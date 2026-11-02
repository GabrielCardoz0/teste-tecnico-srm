package com.srm.creditengine.domain.exchangerate;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class NoCurrentExchangeRateException extends BusinessRuleViolationException {

	public NoCurrentExchangeRateException() {
		super("No current USD/BRL exchange rate to convert the payment to USD");
	}

}
