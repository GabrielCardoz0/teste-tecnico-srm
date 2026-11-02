package com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Golden cases from the challenge statement (section 4.3): base rate 1% a.m.,
 * term in whole months, HALF_EVEN with 2 decimals on the final result only.
 */
class PricingServiceGoldenCasesTest {

	private final PricingService pricingService = new PricingService(
			List.of(new DuplicataMercantilSpread(), new ChequePreDatadoSpread()));

	@Test
	void c1DuplicataMercantilInBrl() {
		PricingResult result = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.BRL, null);

		assertThat(result.presentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(result.discountBrl()).isEqualByComparingTo("7140.06");
		assertThat(result.paymentCurrency()).isEqualTo(PaymentCurrency.BRL);
		assertThat(result.paymentAmount()).isEqualByComparingTo("92859.94");
	}

	@Test
	void c2ChequePreDatadoInBrl() {
		PricingResult result = pricingService.price(ReceivableType.CHEQUE_PRE_DATADO,
				new BigDecimal("25000.00"), 2, PaymentCurrency.BRL, null);

		assertThat(result.presentValueBrl()).isEqualByComparingTo("23337.77");
		assertThat(result.discountBrl()).isEqualByComparingTo("1662.23");
		assertThat(result.paymentAmount()).isEqualByComparingTo("23337.77");
	}

	@Test
	void c3DuplicataMercantilPaidInUsd() {
		PricingResult result = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.USD, new BigDecimal("5.4321"));

		assertThat(result.presentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(result.discountBrl()).isEqualByComparingTo("7140.06");
		assertThat(result.paymentCurrency()).isEqualTo(PaymentCurrency.USD);
		assertThat(result.paymentAmount()).isEqualByComparingTo("17094.67");
	}

}
