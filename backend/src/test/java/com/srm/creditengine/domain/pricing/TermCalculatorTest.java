package com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TermCalculatorTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private static TermCalculator calculatorOn(LocalDate today) {
		Clock clock = Clock.fixed(today.atTime(12, 0).atZone(SAO_PAULO).toInstant(), SAO_PAULO);
		return new TermCalculator(clock);
	}

	@Test
	void exactMonths() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 15));

		assertThat(calculator.termInMonths(LocalDate.of(2026, 4, 15))).isEqualTo(3);
	}

	@Test
	void incompleteMonthRoundsUp() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 15));

		assertThat(calculator.termInMonths(LocalDate.of(2026, 4, 28))).isEqualTo(4);
	}

	@Test
	void endOfMonthCountsAsOneMonth() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 31));

		assertThat(calculator.termInMonths(LocalDate.of(2026, 2, 28))).isEqualTo(1);
	}

	@Test
	void lessThanOneMonthRoundsUpToOne() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 15));

		assertThat(calculator.termInMonths(LocalDate.of(2026, 1, 20))).isEqualTo(1);
	}

	@Test
	void rejectsDueDateToday() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 15));

		assertThatThrownBy(() -> calculator.termInMonths(LocalDate.of(2026, 1, 15)))
				.isInstanceOf(InvalidTermException.class);
	}

	@Test
	void rejectsOverdueDate() {
		TermCalculator calculator = calculatorOn(LocalDate.of(2026, 1, 15));

		assertThatThrownBy(() -> calculator.termInMonths(LocalDate.of(2026, 1, 14)))
				.isInstanceOf(InvalidTermException.class);
	}

}
