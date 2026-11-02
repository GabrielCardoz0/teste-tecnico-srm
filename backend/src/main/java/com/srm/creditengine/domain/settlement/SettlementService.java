package com.srm.creditengine.domain.settlement;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.srm.creditengine.domain.exchangerate.ExchangeRateService;
import com.srm.creditengine.domain.exchangerate.NoCurrentExchangeRateException;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.domain.receivable.ReceivableAlreadySettledException;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;
import com.srm.creditengine.infrastructure.persistence.receivable.ReceivableRepository;
import com.srm.creditengine.infrastructure.persistence.settlement.Settlement;
import com.srm.creditengine.infrastructure.persistence.settlement.SettlementRepository;

@Service
public class SettlementService {

	static final String IDEMPOTENCY_KEY_CONSTRAINT = "uq_settlement_idempotency_key";
	static final String RECEIVABLE_CONSTRAINT = "uq_settlement_receivable";

	private final SettlementRepository settlementRepository;
	private final ReceivableRepository receivableRepository;
	private final TermCalculator termCalculator;
	private final PricingService pricingService;
	private final ExchangeRateService exchangeRateService;
	private final Clock clock;
	private final TransactionTemplate writeTransaction;
	private final TransactionTemplate readTransaction;

	public SettlementService(SettlementRepository settlementRepository, ReceivableRepository receivableRepository,
			TermCalculator termCalculator, PricingService pricingService, ExchangeRateService exchangeRateService,
			Clock clock, PlatformTransactionManager transactionManager) {
		this.settlementRepository = settlementRepository;
		this.receivableRepository = receivableRepository;
		this.termCalculator = termCalculator;
		this.pricingService = pricingService;
		this.exchangeRateService = exchangeRateService;
		this.clock = clock;
		this.writeTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setReadOnly(true);
	}

	public SettlementOutcome settle(UUID idempotencyKey, Long receivableId, PaymentCurrency paymentCurrency) {
		Optional<Settlement> existing = findByKey(idempotencyKey);
		if (existing.isPresent()) {
			return replay(existing.get(), receivableId, paymentCurrency);
		}

		try {
			return SettlementOutcome.created(writeTransaction
					.execute(status -> settleInTransaction(idempotencyKey, receivableId, paymentCurrency)));
		} catch (ReceivableAlreadySettledException conflict) {
			return resolveConflict(idempotencyKey, receivableId, paymentCurrency, conflict);
		} catch (DataIntegrityViolationException violation) {
			String constraint = violatedConstraint(violation);
			if (!IDEMPOTENCY_KEY_CONSTRAINT.equals(constraint) && !RECEIVABLE_CONSTRAINT.equals(constraint)) {
				throw violation;
			}
			return resolveConflict(idempotencyKey, receivableId, paymentCurrency,
					new ReceivableAlreadySettledException(receivableId));
		}
	}

	public Settlement findById(Long id) {
		return readTransaction.execute(status -> settlementRepository.findById(id))
				.orElseThrow(() -> new SettlementNotFoundException(id));
	}

	private Settlement settleInTransaction(UUID idempotencyKey, Long receivableId,
			PaymentCurrency paymentCurrency) {
		Receivable receivable = receivableRepository.findById(receivableId)
				.orElseThrow(() -> new UnknownReceivableException(receivableId));
		if (receivable.getStatus() != ReceivableStatus.PENDING) {
			throw new ReceivableAlreadySettledException(receivableId);
		}

		int termMonths = termCalculator.termInMonths(receivable.getDueDate());
		ExchangeRate usedRate = paymentCurrency == PaymentCurrency.USD
				? exchangeRateService.findCurrent().orElseThrow(NoCurrentExchangeRateException::new)
				: null;
		PricingResult pricing = pricingService.price(receivable.getType(), receivable.getFaceValue(), termMonths,
				paymentCurrency, usedRate != null ? usedRate.getRate() : null);

		receivable.markSettled();
		Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
		return settlementRepository.save(new Settlement(idempotencyKey, receivable, termMonths, pricing, usedRate, now));
	}

	private SettlementOutcome resolveConflict(UUID idempotencyKey, Long receivableId,
			PaymentCurrency paymentCurrency, ReceivableAlreadySettledException conflict) {
		Settlement winner = findByKey(idempotencyKey).orElseThrow(() -> conflict);
		return replay(winner, receivableId, paymentCurrency);
	}

	private SettlementOutcome replay(Settlement settlement, Long receivableId, PaymentCurrency paymentCurrency) {
		if (!settlement.getReceivableId().equals(receivableId) || settlement.getPaymentCurrency() != paymentCurrency) {
			throw new IdempotencyKeyReuseException(settlement.getIdempotencyKey());
		}
		return SettlementOutcome.replayed(settlement);
	}

	private Optional<Settlement> findByKey(UUID idempotencyKey) {
		return readTransaction.execute(status -> settlementRepository.findByIdempotencyKey(idempotencyKey));
	}

	private static String violatedConstraint(Throwable violation) {
		for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				return constraintViolation.getConstraintName();
			}
		}
		return null;
	}

}
