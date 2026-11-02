package com.srm.creditengine.api.settlement;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

record StatementPeriod(Instant fromInclusive, Instant toExclusive) {

	static StatementPeriod of(LocalDate from, LocalDate to, ZoneId zone) {
		if (from != null && to != null && from.isAfter(to)) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
					"from (" + from + ") must not be after to (" + to + ")");
			problem.setTitle("Invalid statement period");
			throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
		}
		return new StatementPeriod(
				from != null ? from.atStartOfDay(zone).toInstant() : null,
				to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null);
	}

}
