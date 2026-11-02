package com.srm.creditengine.api.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.web.ErrorResponseException;

class StatementPeriodTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	@Test
	void convertsSaoPauloDaysToHalfOpenInstantRange() {
		StatementPeriod period = StatementPeriod.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), SAO_PAULO);

		assertThat(period.fromInclusive()).isEqualTo(Instant.parse("2026-11-01T03:00:00Z"));
		assertThat(period.toExclusive()).isEqualTo(Instant.parse("2026-11-03T03:00:00Z"));
	}

	@Test
	void sameDayCoversTheWholeDay() {
		StatementPeriod period = StatementPeriod.of(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2), SAO_PAULO);

		assertThat(period.fromInclusive()).isEqualTo(Instant.parse("2026-11-02T03:00:00Z"));
		assertThat(period.toExclusive()).isEqualTo(Instant.parse("2026-11-03T03:00:00Z"));
	}

	@Test
	void missingBoundsStayOpen() {
		assertThat(StatementPeriod.of(null, null, SAO_PAULO)).isEqualTo(new StatementPeriod(null, null));
		assertThat(StatementPeriod.of(LocalDate.of(2026, 11, 1), null, SAO_PAULO).toExclusive()).isNull();
		assertThat(StatementPeriod.of(null, LocalDate.of(2026, 11, 2), SAO_PAULO).fromInclusive()).isNull();
	}

	@Test
	void fromAfterToIsRejected() {
		assertThatThrownBy(() -> StatementPeriod.of(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2), SAO_PAULO))
				.isInstanceOfSatisfying(ErrorResponseException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
	}

}
