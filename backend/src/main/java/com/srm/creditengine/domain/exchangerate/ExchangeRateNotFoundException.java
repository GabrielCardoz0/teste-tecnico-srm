package com.srm.creditengine.domain.exchangerate;

public class ExchangeRateNotFoundException extends RuntimeException {

	private ExchangeRateNotFoundException(String message) {
		super(message);
	}

	public static ExchangeRateNotFoundException noCurrentRate(CurrencyCode source, CurrencyCode target) {
		return new ExchangeRateNotFoundException("No current " + source + "/" + target + " exchange rate");
	}

	public static ExchangeRateNotFoundException byId(Long id) {
		return new ExchangeRateNotFoundException("Exchange rate " + id + " not found");
	}

}
