package com.srm.creditengine.domain.pricing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class TermCalculator {

	private final Clock clock;

	public TermCalculator(Clock clock) {
		this.clock = clock;
	}

	public int termInMonths(LocalDate dueDate) {
		LocalDate today = LocalDate.now(clock);
		if (!dueDate.isAfter(today)) {
			throw new InvalidTermException(dueDate, today);
		}

		long months = ChronoUnit.MONTHS.between(today, dueDate);
		if (today.plusMonths(months).isBefore(dueDate)) {
			months++;
		}
		return Math.toIntExact(months);
	}

}
