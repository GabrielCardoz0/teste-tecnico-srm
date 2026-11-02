package com.srm.creditengine.infrastructure.persistence.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.srm.creditengine.TestcontainersConfiguration;
import com.srm.creditengine.domain.exchangerate.CurrencyCode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExchangeRateRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	@Autowired
	ExchangeRateRepository repository;

	@Test
	void mostRecentValidRateWins() {
		save("5.10000000", NOW.minus(Duration.ofDays(2)));
		save("5.20000000", NOW.minus(Duration.ofDays(1)));

		assertThat(current()).get().extracting(ExchangeRate::getRate).isEqualTo(new BigDecimal("5.20000000"));
	}

	@Test
	void futureRateIsIgnored() {
		save("5.20000000", NOW.minus(Duration.ofDays(1)));
		save("5.90000000", NOW.plus(Duration.ofHours(1)));

		assertThat(current()).get().extracting(ExchangeRate::getRate).isEqualTo(new BigDecimal("5.20000000"));
	}

	@Test
	void rateValidExactlyNowIsCurrent() {
		save("5.20000000", NOW.minus(Duration.ofDays(1)));
		save("5.30000000", NOW);

		assertThat(current()).get().extracting(ExchangeRate::getRate).isEqualTo(new BigDecimal("5.30000000"));
	}

	@Test
	void tieOnValidFromIsBrokenByLastRegistered() {
		Instant sameValidFrom = NOW.minus(Duration.ofHours(1));
		ExchangeRate first = save("5.30000000", sameValidFrom);
		ExchangeRate last = save("5.31000000", sameValidFrom);

		assertThat(last.getId()).isGreaterThan(first.getId());
		assertThat(current()).get().extracting(ExchangeRate::getId).isEqualTo(last.getId());
	}

	@Test
	void noCurrentRateWhenOnlyFutureRatesExist() {
		save("5.90000000", NOW.plus(Duration.ofHours(1)));

		assertThat(current()).isEmpty();
	}

	@Test
	void noCurrentRateWhenTableIsEmpty() {
		assertThat(current()).isEmpty();
	}

	private ExchangeRate save(String rate, Instant validFrom) {
		return repository.save(new ExchangeRate(CurrencyCode.USD, CurrencyCode.BRL, new BigDecimal(rate),
				validFrom, NOW));
	}

	private Optional<ExchangeRate> current() {
		return repository.findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
				CurrencyCode.USD, CurrencyCode.BRL, NOW);
	}

}
