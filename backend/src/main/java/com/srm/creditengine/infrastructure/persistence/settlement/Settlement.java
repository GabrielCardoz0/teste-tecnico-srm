package com.srm.creditengine.infrastructure.persistence.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;

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
@Table(name = "settlement")
public class Settlement {

	private static final int RATE_SCALE = 8;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "idempotency_key", nullable = false)
	private UUID idempotencyKey;

	@Column(name = "receivable_id", nullable = false)
	private Long receivableId;

	@Enumerated(EnumType.STRING)
	@Column(name = "receivable_type", nullable = false, length = 30)
	private ReceivableType receivableType;

	@Column(name = "face_value", nullable = false, precision = 19, scale = 2)
	private BigDecimal faceValue;

	@Column(name = "term_months", nullable = false)
	private int termMonths;

	@Column(name = "base_rate", nullable = false, precision = 19, scale = 8)
	private BigDecimal baseRate;

	@Column(name = "spread", nullable = false, precision = 19, scale = 8)
	private BigDecimal spread;

	@Column(name = "present_value_brl", nullable = false, precision = 19, scale = 2)
	private BigDecimal presentValueBrl;

	@Column(name = "discount_brl", nullable = false, precision = 19, scale = 2)
	private BigDecimal discountBrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_currency", nullable = false, length = 3)
	private PaymentCurrency paymentCurrency;

	@Column(name = "exchange_rate_id")
	private Long exchangeRateId;

	@Column(name = "exchange_rate", precision = 19, scale = 8)
	private BigDecimal exchangeRate;

	@Column(name = "payment_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal paymentAmount;

	@Column(name = "settled_at", nullable = false)
	private Instant settledAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Settlement() {
	}

	public Settlement(UUID idempotencyKey, Receivable receivable, int termMonths, PricingResult pricing,
			ExchangeRate usedRate, Instant settledAt) {
		this.idempotencyKey = idempotencyKey;
		this.receivableId = receivable.getId();
		this.receivableType = receivable.getType();
		this.faceValue = receivable.getFaceValue();
		this.termMonths = termMonths;
		this.baseRate = pricing.baseRate().setScale(RATE_SCALE);
		this.spread = pricing.spread().setScale(RATE_SCALE);
		this.presentValueBrl = pricing.presentValueBrl();
		this.discountBrl = pricing.discountBrl();
		this.paymentCurrency = pricing.paymentCurrency();
		this.exchangeRateId = usedRate != null ? usedRate.getId() : null;
		this.exchangeRate = usedRate != null ? usedRate.getRate() : null;
		this.paymentAmount = pricing.paymentAmount();
		this.settledAt = settledAt;
		this.createdAt = settledAt;
	}

	public Long getId() {
		return id;
	}

	public UUID getIdempotencyKey() {
		return idempotencyKey;
	}

	public Long getReceivableId() {
		return receivableId;
	}

	public ReceivableType getReceivableType() {
		return receivableType;
	}

	public BigDecimal getFaceValue() {
		return faceValue;
	}

	public int getTermMonths() {
		return termMonths;
	}

	public BigDecimal getBaseRate() {
		return baseRate;
	}

	public BigDecimal getSpread() {
		return spread;
	}

	public BigDecimal getPresentValueBrl() {
		return presentValueBrl;
	}

	public BigDecimal getDiscountBrl() {
		return discountBrl;
	}

	public PaymentCurrency getPaymentCurrency() {
		return paymentCurrency;
	}

	public Long getExchangeRateId() {
		return exchangeRateId;
	}

	public BigDecimal getExchangeRate() {
		return exchangeRate;
	}

	public BigDecimal getPaymentAmount() {
		return paymentAmount;
	}

	public Instant getSettledAt() {
		return settledAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
