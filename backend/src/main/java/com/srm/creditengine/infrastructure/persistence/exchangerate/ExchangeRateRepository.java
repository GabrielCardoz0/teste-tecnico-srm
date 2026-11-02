package com.srm.creditengine.infrastructure.persistence.exchangerate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.srm.creditengine.domain.exchangerate.CurrencyCode;

public interface ExchangeRateRepository extends Repository<ExchangeRate, Long> {

	ExchangeRate save(ExchangeRate exchangeRate);

	Optional<ExchangeRate> findById(Long id);

	List<ExchangeRate> findAllByOrderByValidFromDescIdDesc();

	Optional<ExchangeRate> findFirstBySourceCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(
			CurrencyCode sourceCurrency, CurrencyCode targetCurrency, Instant now);

}
