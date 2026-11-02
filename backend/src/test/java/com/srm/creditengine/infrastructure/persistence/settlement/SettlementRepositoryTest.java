package com.srm.creditengine.infrastructure.persistence.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.srm.creditengine.TestcontainersConfiguration;
import com.srm.creditengine.domain.exchangerate.CurrencyCode;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.PricingResult;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.receivable.ReceivableAlreadySettledException;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.assignor.AssignorRepository;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRateRepository;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;
import com.srm.creditengine.infrastructure.persistence.receivable.ReceivableRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SettlementRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	@Autowired
	SettlementRepository settlementRepository;

	@Autowired
	ReceivableRepository receivableRepository;

	@Autowired
	ExchangeRateRepository exchangeRateRepository;

	@Autowired
	AssignorRepository assignorRepository;

	@Autowired
	TestEntityManager entityManager;

	private Receivable receivable;

	@BeforeEach
	void setUp() {
		Long assignorId = assignorRepository.findAllByActiveTrueOrderByLegalNameAsc().get(0).getId();
		receivable = receivableRepository.save(new Receivable(assignorId, ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100000.00"), LocalDate.of(2026, 4, 15), ReceivableStatus.PENDING, NOW));
	}

	@Test
	void usdSettlementRecordsEverythingNeededToRebuildTheCalculation() {
		ExchangeRate rate = exchangeRateRepository.save(new ExchangeRate(CurrencyCode.USD, CurrencyCode.BRL,
				new BigDecimal("5.43210000"), NOW, NOW));
		PricingResult pricing = new PricingResult(new BigDecimal("0.01"), new BigDecimal("0.015"),
				new BigDecimal("92859.94"), new BigDecimal("7140.06"), PaymentCurrency.USD,
				new BigDecimal("17094.67"));
		UUID key = UUID.randomUUID();
		settlementRepository.save(new Settlement(key, receivable, 3, pricing, rate, NOW));
		entityManager.flush();
		entityManager.clear();

		Settlement stored = settlementRepository.findByIdempotencyKey(key).orElseThrow();

		assertThat(stored.getReceivableId()).isEqualTo(receivable.getId());
		assertThat(stored.getReceivableType()).isEqualTo(ReceivableType.DUPLICATA_MERCANTIL);
		assertThat(stored.getFaceValue()).isEqualByComparingTo("100000.00");
		assertThat(stored.getTermMonths()).isEqualTo(3);
		assertThat(stored.getBaseRate()).isEqualByComparingTo("0.01");
		assertThat(stored.getSpread()).isEqualByComparingTo("0.015");
		assertThat(stored.getPresentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(stored.getDiscountBrl()).isEqualByComparingTo("7140.06");
		assertThat(stored.getPaymentCurrency()).isEqualTo(PaymentCurrency.USD);
		assertThat(stored.getExchangeRateId()).isEqualTo(rate.getId());
		assertThat(stored.getExchangeRate()).isEqualByComparingTo("5.4321");
		assertThat(stored.getPaymentAmount()).isEqualByComparingTo("17094.67");
		assertThat(stored.getSettledAt()).isEqualTo(NOW);
		assertThat(stored.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void markSettledIsPersisted() {
		receivable.markSettled();
		entityManager.flush();
		entityManager.clear();

		assertThat(receivableRepository.findById(receivable.getId()).orElseThrow().getStatus())
				.isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void settledReceivableCannotBeSettledAgain() {
		receivable.markSettled();

		assertThatThrownBy(receivable::markSettled).isInstanceOf(ReceivableAlreadySettledException.class);
	}

}
