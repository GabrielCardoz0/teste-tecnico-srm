package com.srm.creditengine.domain.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRateRepository;

class ExchangeRateServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	private final ExchangeRateRepository repository = mock(ExchangeRateRepository.class);

	private final ExchangeRateService service = new ExchangeRateService(repository,
			Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo")));

	@Test
	void missingValidFromDefaultsToNow() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ExchangeRate saved = service.register(CurrencyCode.USD, CurrencyCode.BRL, new BigDecimal("5.4321"), null);

		assertThat(saved.getValidFrom()).isEqualTo(NOW);
		assertThat(saved.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void informedValidFromIsKept() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Instant validFrom = Instant.parse("2026-01-20T12:00:00Z");

		ExchangeRate saved = service.register(CurrencyCode.USD, CurrencyCode.BRL, new BigDecimal("5.4321"),
				validFrom);

		assertThat(saved.getValidFrom()).isEqualTo(validFrom);
		assertThat(saved.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void currentLooksUpUsdBrlAtClockInstant() {
		ExchangeRate rate = new ExchangeRate(CurrencyCode.USD, CurrencyCode.BRL, new BigDecimal("5.4321"), NOW, NOW);
		when(repository.findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
				CurrencyCode.USD, CurrencyCode.BRL, NOW)).thenReturn(Optional.of(rate));

		assertThat(service.current()).isSameAs(rate);
		verify(repository).findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
				CurrencyCode.USD, CurrencyCode.BRL, NOW);
	}

	@Test
	void currentThrowsWhenThereIsNoValidRate() {
		when(repository.findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
				CurrencyCode.USD, CurrencyCode.BRL, NOW)).thenReturn(Optional.empty());

		assertThatThrownBy(service::current).isInstanceOf(ExchangeRateNotFoundException.class);
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(42L)).isInstanceOf(ExchangeRateNotFoundException.class);
	}

}
