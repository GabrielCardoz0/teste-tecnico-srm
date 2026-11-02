package com.srm.creditengine.domain.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.srm.creditengine.domain.exchangerate.ExchangeRateService;
import com.srm.creditengine.domain.exchangerate.NoCurrentExchangeRateException;
import com.srm.creditengine.domain.pricing.ChequePreDatadoSpread;
import com.srm.creditengine.domain.pricing.DuplicataMercantilSpread;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.domain.receivable.ReceivableAlreadySettledException;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;
import com.srm.creditengine.infrastructure.persistence.receivable.ReceivableRepository;
import com.srm.creditengine.infrastructure.persistence.settlement.Settlement;
import com.srm.creditengine.infrastructure.persistence.settlement.SettlementRepository;

class SettlementServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo"));

	private static final UUID KEY = UUID.randomUUID();

	private static final Long RECEIVABLE_ID = 1L;

	private final SettlementRepository settlementRepository = mock(SettlementRepository.class);

	private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);

	private final ExchangeRateService exchangeRateService = mock(ExchangeRateService.class);

	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

	private final SettlementService service = new SettlementService(settlementRepository, receivableRepository,
			new TermCalculator(CLOCK),
			new PricingService(List.of(new DuplicataMercantilSpread(), new ChequePreDatadoSpread())),
			exchangeRateService, CLOCK, transactionManager);

	private Receivable receivable;

	@BeforeEach
	void setUp() {
		when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
		receivable = new Receivable(10L, ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000.00"),
				LocalDate.of(2026, 4, 15), ReceivableStatus.PENDING, NOW);
		when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
		when(settlementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
	}

	@Test
	void firstRequestCreatesSettlementAndSettlesReceivable() {
		when(settlementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		SettlementOutcome outcome = service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL);

		assertThat(outcome.created()).isTrue();
		assertThat(outcome.settlement().getTermMonths()).isEqualTo(3);
		assertThat(outcome.settlement().getPresentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(outcome.settlement().getDiscountBrl()).isEqualByComparingTo("7140.06");
		assertThat(outcome.settlement().getSettledAt()).isEqualTo(NOW);
		assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void sameKeyAndBodyReplaysExistingSettlementWithoutWriting() {
		Settlement existing = settlementFor(RECEIVABLE_ID, PaymentCurrency.BRL);
		when(settlementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

		SettlementOutcome outcome = service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL);

		assertThat(outcome.created()).isFalse();
		assertThat(outcome.settlement()).isSameAs(existing);
		verify(settlementRepository, never()).save(any());
	}

	@Test
	void sameKeyWithDifferentBodyIsRejected() {
		Settlement existing = settlementFor(RECEIVABLE_ID, PaymentCurrency.BRL);
		when(settlementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.USD))
				.isInstanceOf(IdempotencyKeyReuseException.class);
	}

	@Test
	void unknownReceivableIsRejected() {
		when(receivableRepository.findById(RECEIVABLE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL))
				.isInstanceOf(UnknownReceivableException.class);
	}

	@Test
	void receivableSettledByAnotherKeyIsAConflict() {
		receivable.markSettled();

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL))
				.isInstanceOf(ReceivableAlreadySettledException.class);
	}

	@Test
	void receivableSettledByConcurrentRequestWithSameKeyIsReplayed() {
		receivable.markSettled();
		Settlement winner = settlementFor(RECEIVABLE_ID, PaymentCurrency.BRL);
		when(settlementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty(), Optional.of(winner));

		SettlementOutcome outcome = service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL);

		assertThat(outcome.created()).isFalse();
		assertThat(outcome.settlement()).isSameAs(winner);
	}

	@Test
	void idempotencyKeyViolationReplaysTheConcurrentWinner() {
		Settlement winner = settlementFor(RECEIVABLE_ID, PaymentCurrency.BRL);
		when(settlementRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty(), Optional.of(winner));
		when(settlementRepository.save(any()))
				.thenThrow(violation(SettlementService.IDEMPOTENCY_KEY_CONSTRAINT));

		SettlementOutcome outcome = service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL);

		assertThat(outcome.created()).isFalse();
		assertThat(outcome.settlement()).isSameAs(winner);
		verify(transactionManager).rollback(any());
	}

	@Test
	void idempotencyKeyViolationWithDifferentBodyIsRejected() {
		Settlement winnerForOtherReceivable = settlementFor(2L, PaymentCurrency.BRL);
		when(settlementRepository.findByIdempotencyKey(KEY))
				.thenReturn(Optional.empty(), Optional.of(winnerForOtherReceivable));
		when(settlementRepository.save(any()))
				.thenThrow(violation(SettlementService.IDEMPOTENCY_KEY_CONSTRAINT));

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL))
				.isInstanceOf(IdempotencyKeyReuseException.class);
	}

	@Test
	void receivableViolationWithoutWinnerForKeyIsAConflict() {
		when(settlementRepository.save(any())).thenThrow(violation(SettlementService.RECEIVABLE_CONSTRAINT));

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL))
				.isInstanceOf(ReceivableAlreadySettledException.class);
		verify(transactionManager).rollback(any());
	}

	@Test
	void unexpectedConstraintViolationIsRethrown() {
		when(settlementRepository.save(any())).thenThrow(violation("ck_settlement_positive_values"));

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.BRL))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void usdWithoutCurrentRateIsRejectedWithoutWriting() {
		when(exchangeRateService.findCurrent()).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.settle(KEY, RECEIVABLE_ID, PaymentCurrency.USD))
				.isInstanceOf(NoCurrentExchangeRateException.class);
		verify(settlementRepository, never()).save(any());
	}

	@Test
	void unknownSettlementIdIsNotFound() {
		when(settlementRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(SettlementNotFoundException.class);
	}

	private static DataIntegrityViolationException violation(String constraint) {
		return new DataIntegrityViolationException("duplicate",
				new ConstraintViolationException("duplicate", new SQLException("duplicate"), constraint));
	}

	private Settlement settlementFor(Long receivableId, PaymentCurrency currency) {
		Receivable settled = mock(Receivable.class);
		when(settled.getId()).thenReturn(receivableId);
		when(settled.getType()).thenReturn(ReceivableType.DUPLICATA_MERCANTIL);
		when(settled.getFaceValue()).thenReturn(new BigDecimal("100000.00"));
		PricingResult pricing = new PricingResult(new BigDecimal("0.01"), new BigDecimal("0.015"),
				new BigDecimal("92859.94"), new BigDecimal("7140.06"), currency, new BigDecimal("92859.94"));
		return new Settlement(KEY, settled, 3, pricing, null, NOW);
	}

}
