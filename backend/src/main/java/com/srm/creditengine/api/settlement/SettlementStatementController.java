package com.srm.creditengine.api.settlement;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.api.PageResponse;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.infrastructure.persistence.settlement.SettlementStatementFilter;
import com.srm.creditengine.infrastructure.persistence.settlement.SettlementStatementRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/settlements")
public class SettlementStatementController {

	private final SettlementStatementRepository repository;
	private final Clock clock;

	public SettlementStatementController(SettlementStatementRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@GetMapping
	PageResponse<SettlementStatementLine> statement(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long assignorId,
			@RequestParam(required = false) PaymentCurrency paymentCurrency,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		StatementPeriod period = StatementPeriod.of(from, to, clock.getZone());
		SettlementStatementFilter filter = new SettlementStatementFilter(period.fromInclusive(), period.toExclusive(),
				assignorId, paymentCurrency);
		return PageResponse.from(repository.find(filter, PageRequest.of(page, size)), SettlementStatementLine::from);
	}

}
