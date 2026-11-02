package com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PricingServiceTest {

	private static final BigDecimal RATE = new BigDecimal("4.0000");

	private final PricingService pricingService = new PricingService(
			List.of(new DuplicataMercantilSpread(), new ChequePreDatadoSpread()));

	@Test
	void resultExposesRatesUsedForEachType() {
		PricingResult duplicata = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.BRL, null);
		PricingResult cheque = pricingService.price(ReceivableType.CHEQUE_PRE_DATADO,
				new BigDecimal("25000.00"), 2, PaymentCurrency.BRL, null);

		assertThat(duplicata.baseRate()).isEqualByComparingTo("0.01");
		assertThat(duplicata.spread()).isEqualByComparingTo("0.015");
		assertThat(cheque.baseRate()).isEqualByComparingTo("0.01");
		assertThat(cheque.spread()).isEqualByComparingTo("0.025");
	}

	@Test
	void usdHalfCentRoundsDownToEvenDigit() {
		PricingResult result = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.USD, RATE);

		assertThat(result.presentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(result.paymentAmount()).isEqualByComparingTo("23214.98");
	}

	@Test
	void usdHalfCentRoundsUpToEvenDigit() {
		PricingResult result = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("30000.00"), 3, PaymentCurrency.USD, RATE);

		assertThat(result.presentValueBrl()).isEqualByComparingTo("27857.98");
		assertThat(result.paymentAmount()).isEqualByComparingTo("6964.50");
	}

	@Test
	void brlPaymentIgnoresExchangeRate() {
		PricingResult result = pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.BRL, RATE);

		assertThat(result.paymentAmount()).isEqualByComparingTo("92859.94");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-0.01" })
	void rejectsNonPositiveFaceValue(String faceValue) {
		assertThatThrownBy(() -> pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal(faceValue), 3, PaymentCurrency.BRL, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1 })
	void rejectsNonPositiveTerm(int termMonths) {
		assertThatThrownBy(() -> pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), termMonths, PaymentCurrency.BRL, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void usdRequiresExchangeRate() {
		assertThatThrownBy(() -> pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.USD, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void usdRejectsNonPositiveExchangeRate() {
		assertThatThrownBy(() -> pricingService.price(ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), 3, PaymentCurrency.USD, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsTypeWithoutStrategy() {
		PricingService onlyDuplicata = new PricingService(List.of(new DuplicataMercantilSpread()));

		assertThatThrownBy(() -> onlyDuplicata.price(ReceivableType.CHEQUE_PRE_DATADO,
				new BigDecimal("25000.00"), 2, PaymentCurrency.BRL, null))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsDuplicateStrategyForSameType() {
		assertThatThrownBy(() -> new PricingService(
				List.of(new DuplicataMercantilSpread(), new DuplicataMercantilSpread())))
				.isInstanceOf(IllegalStateException.class);
	}

}
