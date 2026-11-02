package com.srm.creditengine.domain.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.srm.creditengine.domain.exchangerate.CurrencyCode;
import com.srm.creditengine.domain.exchangerate.ExchangeRateService;
import com.srm.creditengine.domain.exchangerate.NoCurrentExchangeRateException;
import com.srm.creditengine.domain.pricing.ChequePreDatadoSpread;
import com.srm.creditengine.domain.pricing.DuplicataMercantilSpread;
import com.srm.creditengine.domain.pricing.InvalidTermException;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

class SimulationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo"));

	private final ExchangeRateService exchangeRateService = mock(ExchangeRateService.class);

	private final SimulationService service = new SimulationService(new TermCalculator(CLOCK),
			new PricingService(List.of(new DuplicataMercantilSpread(), new ChequePreDatadoSpread())),
			exchangeRateService);

	@Test
	void brlSimulationDoesNotLookUpExchangeRate() {
		SimulationResult result = service.simulate(ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000.00"),
				LocalDate.of(2026, 4, 15), PaymentCurrency.BRL);

		assertThat(result.termMonths()).isEqualTo(3);
		assertThat(result.pricing().presentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(result.exchangeRate()).isNull();
		verifyNoInteractions(exchangeRateService);
	}

	@Test
	void usdSimulationUsesCurrentExchangeRate() {
		ExchangeRate rate = new ExchangeRate(CurrencyCode.USD, CurrencyCode.BRL, new BigDecimal("5.43210000"), NOW,
				NOW);
		when(exchangeRateService.findCurrent()).thenReturn(Optional.of(rate));

		SimulationResult result = service.simulate(ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000.00"),
				LocalDate.of(2026, 4, 15), PaymentCurrency.USD);

		assertThat(result.pricing().paymentAmount()).isEqualByComparingTo("17094.67");
		assertThat(result.exchangeRate()).isSameAs(rate);
	}

	@Test
	void usdSimulationWithoutCurrentRateIsRejected() {
		when(exchangeRateService.findCurrent()).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.simulate(ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000.00"),
				LocalDate.of(2026, 4, 15), PaymentCurrency.USD))
				.isInstanceOf(NoCurrentExchangeRateException.class);
	}

	@Test
	void dueDateTodayIsRejectedBeforeLookingUpExchangeRate() {
		assertThatThrownBy(() -> service.simulate(ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000.00"),
				LocalDate.of(2026, 1, 15), PaymentCurrency.USD))
				.isInstanceOf(InvalidTermException.class);
		verifyNoInteractions(exchangeRateService);
	}

}
