package com.srm.creditengine.infrastructure.persistence.exchangerate;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import com.srm.creditengine.domain.exchangerate.CurrencyCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "exchange_rate")
public class ExchangeRate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_currency", nullable = false, length = 3)
	private CurrencyCode sourceCurrency;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_currency", nullable = false, length = 3)
	private CurrencyCode targetCurrency;

	@Column(name = "rate", nullable = false, precision = 19, scale = 8)
	private BigDecimal rate;

	@Column(name = "valid_from", nullable = false)
	private Instant validFrom;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ExchangeRate() {
	}

	public ExchangeRate(CurrencyCode sourceCurrency, CurrencyCode targetCurrency, BigDecimal rate,
			Instant validFrom, Instant createdAt) {
		this.sourceCurrency = sourceCurrency;
		this.targetCurrency = targetCurrency;
		this.rate = rate;
		this.validFrom = validFrom;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public CurrencyCode getSourceCurrency() {
		return sourceCurrency;
	}

	public CurrencyCode getTargetCurrency() {
		return targetCurrency;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public Instant getValidFrom() {
		return validFrom;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
