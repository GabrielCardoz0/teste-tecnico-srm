package com.srm.creditengine.infrastructure.persistence.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.srm.creditengine.TestcontainersConfiguration;
import com.srm.creditengine.domain.pricing.PaymentCurrency;
import com.srm.creditengine.domain.pricing.ReceivableType;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, SettlementStatementRepository.class })
class SettlementStatementRepositoryTest {

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

	private static final Instant NOV_01_START = Instant.parse("2026-11-01T03:00:00Z");
	private static final Instant NOV_01_AFTERNOON = Instant.parse("2026-11-01T15:00:00Z");
	private static final Instant NOV_02_NOON = Instant.parse("2026-11-02T12:00:00Z");
	private static final Instant NOV_02_LAST_MINUTE = Instant.parse("2026-11-03T02:59:00Z");
	private static final Instant NOV_03_START = Instant.parse("2026-11-03T03:00:00Z");

	@Autowired
	SettlementStatementRepository repository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Long alfaId;
	private Long betaId;
	private Long gamaId;
	private Long rateId;

	private Long s1;
	private Long s2;
	private Long s3;
	private Long s4;
	private Long s5;

	@BeforeEach
	void setUp() {
		alfaId = assignorId("11222333000181");
		betaId = assignorId("12ABC34501DE35");
		gamaId = assignorId("B7C8D9E0000120");
		rateId = jdbcTemplate.queryForObject("""
				INSERT INTO exchange_rate (source_currency, target_currency, rate, valid_from, created_at)
				VALUES ('USD', 'BRL', 5.4321, ?, ?) RETURNING id
				""", Long.class, Timestamp.from(NOV_01_START), Timestamp.from(NOV_01_START));

		s1 = insertSettlement(alfaId, PaymentCurrency.BRL, NOV_01_START);
		s2 = insertSettlement(betaId, PaymentCurrency.USD, NOV_01_AFTERNOON);
		s3 = insertSettlement(alfaId, PaymentCurrency.USD, NOV_02_NOON);
		s4 = insertSettlement(betaId, PaymentCurrency.BRL, NOV_02_LAST_MINUTE);
		s5 = insertSettlement(alfaId, PaymentCurrency.BRL, NOV_03_START);
	}

	@Test
	void withoutFiltersReturnsEverythingNewestFirst() {
		Page<SettlementStatementRow> page = find(new SettlementStatementFilter(null, null, null, null), FIRST_PAGE);

		assertThat(page.getTotalElements()).isEqualTo(5);
		assertThat(page.getContent()).extracting(SettlementStatementRow::settlementId)
				.containsExactly(s5, s4, s3, s2, s1);
	}

	@Test
	void rowCarriesAssignorAndCalculationValues() {
		SettlementStatementRow row = find(new SettlementStatementFilter(null, null, betaId, PaymentCurrency.USD),
				FIRST_PAGE).getContent().get(0);

		assertThat(row.settlementId()).isEqualTo(s2);
		assertThat(row.settledAt()).isEqualTo(NOV_01_AFTERNOON);
		assertThat(row.assignorLegalName()).isEqualTo("Beta Distribuidora de Alimentos S.A.");
		assertThat(row.assignorCnpj()).isEqualTo("12ABC34501DE35");
		assertThat(row.receivableType()).isEqualTo(ReceivableType.DUPLICATA_MERCANTIL);
		assertThat(row.faceValue()).isEqualByComparingTo("100000.00");
		assertThat(row.presentValueBrl()).isEqualByComparingTo("92859.94");
		assertThat(row.discountBrl()).isEqualByComparingTo("7140.06");
		assertThat(row.paymentCurrency()).isEqualTo(PaymentCurrency.USD);
		assertThat(row.exchangeRate()).isEqualByComparingTo("5.4321");
		assertThat(row.paymentAmount()).isEqualByComparingTo("17094.67");
	}

	@Test
	void lowerBoundIsInclusive() {
		Page<SettlementStatementRow> page = find(new SettlementStatementFilter(NOV_01_AFTERNOON, null, null, null),
				FIRST_PAGE);

		assertThat(page.getContent()).extracting(SettlementStatementRow::settlementId)
				.containsExactly(s5, s4, s3, s2);
	}

	@Test
	void upperBoundIsExclusive() {
		Page<SettlementStatementRow> page = find(new SettlementStatementFilter(null, NOV_03_START, null, null),
				FIRST_PAGE);

		assertThat(page.getContent()).extracting(SettlementStatementRow::settlementId)
				.containsExactly(s4, s3, s2, s1);
	}

	@Test
	void filtersByAssignor() {
		Page<SettlementStatementRow> page = find(new SettlementStatementFilter(null, null, betaId, null), FIRST_PAGE);

		assertThat(page.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s4, s2);
	}

	@Test
	void filtersByPaymentCurrencyAndOnlyUsdRowsCarryTheRate() {
		Page<SettlementStatementRow> usd = find(new SettlementStatementFilter(null, null, null, PaymentCurrency.USD),
				FIRST_PAGE);
		Page<SettlementStatementRow> brl = find(new SettlementStatementFilter(null, null, null, PaymentCurrency.BRL),
				FIRST_PAGE);

		assertThat(usd.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s3, s2);
		assertThat(usd.getContent()).allSatisfy(row -> assertThat(row.exchangeRate()).isNotNull());
		assertThat(brl.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s5, s4, s1);
		assertThat(brl.getContent()).allSatisfy(row -> assertThat(row.exchangeRate()).isNull());
	}

	@Test
	void combinesAllFilters() {
		Page<SettlementStatementRow> page = find(
				new SettlementStatementFilter(NOV_01_START, NOV_03_START, alfaId, PaymentCurrency.BRL), FIRST_PAGE);

		assertThat(page.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s1);
	}

	@Test
	void paginatesWithTotalsAndOrder() {
		SettlementStatementFilter all = new SettlementStatementFilter(null, null, null, null);

		Page<SettlementStatementRow> first = find(all, PageRequest.of(0, 2));
		Page<SettlementStatementRow> last = find(all, PageRequest.of(2, 2));
		Page<SettlementStatementRow> beyond = find(all, PageRequest.of(3, 2));

		assertThat(first.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s5, s4);
		assertThat(first.getTotalElements()).isEqualTo(5);
		assertThat(first.getTotalPages()).isEqualTo(3);
		assertThat(last.getContent()).extracting(SettlementStatementRow::settlementId).containsExactly(s1);
		assertThat(beyond.getContent()).isEmpty();
		assertThat(beyond.getTotalElements()).isEqualTo(5);
	}

	@Test
	void emptyResult() {
		Page<SettlementStatementRow> page = find(new SettlementStatementFilter(null, null, gamaId, null), FIRST_PAGE);

		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalElements()).isZero();
		assertThat(page.getTotalPages()).isZero();
	}

	private Page<SettlementStatementRow> find(SettlementStatementFilter filter, PageRequest pageRequest) {
		return repository.find(filter, pageRequest);
	}

	private Long insertSettlement(Long assignorId, PaymentCurrency currency, Instant settledAt) {
		Long receivableId = jdbcTemplate.queryForObject("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				VALUES (?, 'DUPLICATA_MERCANTIL', 100000.00, DATE '2027-01-15', 'SETTLED', ?) RETURNING id
				""", Long.class, assignorId, Timestamp.from(settledAt));
		boolean usd = currency == PaymentCurrency.USD;
		return jdbcTemplate.queryForObject("""
				INSERT INTO settlement (idempotency_key, receivable_id, receivable_type, face_value, term_months,
				    base_rate, spread, present_value_brl, discount_brl, payment_currency, exchange_rate_id,
				    exchange_rate, payment_amount, settled_at, created_at)
				VALUES (?, ?, 'DUPLICATA_MERCANTIL', 100000.00, 3, 0.01, 0.015, 92859.94, 7140.06, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, UUID.randomUUID(), receivableId, currency.name(), usd ? rateId : null,
				usd ? new BigDecimal("5.43210000") : null, usd ? new BigDecimal("17094.67") : new BigDecimal("92859.94"),
				Timestamp.from(settledAt), Timestamp.from(settledAt));
	}

	private Long assignorId(String cnpj) {
		return jdbcTemplate.queryForObject("SELECT id FROM assignor WHERE cnpj = ?", Long.class, cnpj);
	}

}
