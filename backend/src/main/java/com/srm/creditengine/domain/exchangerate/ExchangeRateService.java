package com.srm.creditengine.domain.exchangerate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRateRepository;

@Service
public class ExchangeRateService {

	private final ExchangeRateRepository repository;
	private final Clock clock;

	public ExchangeRateService(ExchangeRateRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public ExchangeRate register(CurrencyCode source, CurrencyCode target, BigDecimal rate, Instant validFrom) {
		Instant now = Instant.now(clock);
		return repository.save(new ExchangeRate(source, target, rate, validFrom != null ? validFrom : now, now));
	}

	@Transactional(readOnly = true)
	public ExchangeRate current() {
		return repository
				.findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
						CurrencyCode.USD, CurrencyCode.BRL, Instant.now(clock))
				.orElseThrow(() -> ExchangeRateNotFoundException.noCurrentRate(CurrencyCode.USD, CurrencyCode.BRL));
	}

	@Transactional(readOnly = true)
	public ExchangeRate findById(Long id) {
		return repository.findById(id).orElseThrow(() -> ExchangeRateNotFoundException.byId(id));
	}

	@Transactional(readOnly = true)
	public List<ExchangeRate> history() {
		return repository.findAllByOrderByValidFromDescIdDesc();
	}

}
