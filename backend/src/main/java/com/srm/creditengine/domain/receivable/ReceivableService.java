package com.srm.creditengine.domain.receivable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.domain.assignor.AssignorService;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;
import com.srm.creditengine.infrastructure.persistence.receivable.ReceivableRepository;

@Service
public class ReceivableService {

	private static final int MONEY_SCALE = 2;

	private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	private final ReceivableRepository repository;
	private final AssignorService assignorService;
	private final TermCalculator termCalculator;
	private final Clock clock;

	public ReceivableService(ReceivableRepository repository, AssignorService assignorService,
			TermCalculator termCalculator, Clock clock) {
		this.repository = repository;
		this.assignorService = assignorService;
		this.termCalculator = termCalculator;
		this.clock = clock;
	}

	@Transactional
	public Receivable register(Long assignorId, ReceivableType type, BigDecimal faceValue, LocalDate dueDate) {
		assignorService.requireActive(assignorId);
		termCalculator.termInMonths(dueDate);

		Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
		return repository.save(new Receivable(assignorId, type, faceValue.setScale(MONEY_SCALE), dueDate,
				ReceivableStatus.PENDING, now));
	}

	@Transactional(readOnly = true)
	public Receivable findById(Long id) {
		return repository.findById(id).orElseThrow(() -> new ReceivableNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<Receivable> search(ReceivableStatus status, Long assignorId, BigDecimal faceValue,
			LocalDate dueDate, int page, int size) {
		return repository.search(status, assignorId, faceValue, dueDate, PageRequest.of(page, size, NEWEST_FIRST));
	}

}
