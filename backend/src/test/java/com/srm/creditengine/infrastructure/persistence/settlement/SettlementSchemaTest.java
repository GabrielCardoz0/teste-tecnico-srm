package com.srm.creditengine.infrastructure.persistence.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.srm.creditengine.TestcontainersConfiguration;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SettlementSchemaTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Long receivableId;
	private Long otherReceivableId;
	private Long exchangeRateId;

	@BeforeEach
	void setUp() {
		receivableId = insertReceivable();
		otherReceivableId = insertReceivable();
		exchangeRateId = jdbcTemplate.queryForObject("""
				INSERT INTO exchange_rate (source_currency, target_currency, rate, valid_from, created_at)
				VALUES ('USD', 'BRL', 5.4321, now(), now()) RETURNING id
				""", Long.class);
	}

	@Test
	void acceptsBrlSettlementWithoutRateAndUsdSettlementWithRate() {
		insertSettlement(UUID.randomUUID(), receivableId, "BRL", null, null, "92859.94");
		insertSettlement(UUID.randomUUID(), otherReceivableId, "USD", exchangeRateId, "5.43210000", "17094.67");

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM settlement", Integer.class)).isEqualTo(2);
	}

	@Test
	void rejectsSecondSettlementForSameReceivable() {
		insertSettlement(UUID.randomUUID(), receivableId, "BRL", null, null, "92859.94");

		assertThatThrownBy(() -> insertSettlement(UUID.randomUUID(), receivableId, "BRL", null, null, "92859.94"))
				.isInstanceOf(DuplicateKeyException.class)
				.hasMessageContaining("uq_settlement_receivable");
	}

	@Test
	void rejectsReusedIdempotencyKey() {
		UUID key = UUID.randomUUID();
		insertSettlement(key, receivableId, "BRL", null, null, "92859.94");

		assertThatThrownBy(() -> insertSettlement(key, otherReceivableId, "BRL", null, null, "92859.94"))
				.isInstanceOf(DuplicateKeyException.class)
				.hasMessageContaining("uq_settlement_idempotency_key");
	}

	@Test
	void rejectsBrlSettlementWithRate() {
		assertThatThrownBy(() -> insertSettlement(UUID.randomUUID(), receivableId, "BRL", exchangeRateId,
				"5.43210000", "92859.94"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_settlement_rate_presence");
	}

	@Test
	void rejectsUsdSettlementWithoutRate() {
		assertThatThrownBy(() -> insertSettlement(UUID.randomUUID(), receivableId, "USD", null, null, "17094.67"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_settlement_rate_presence");
	}

	@Test
	void rejectsNonPositivePaymentAmount() {
		assertThatThrownBy(() -> insertSettlement(UUID.randomUUID(), receivableId, "BRL", null, null, "0.00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_settlement_positive_values");
	}

	private Long insertReceivable() {
		return jdbcTemplate.queryForObject("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				SELECT id, 'DUPLICATA_MERCANTIL', 100000.00, DATE '2026-04-15', 'PENDING', now()
				FROM assignor WHERE cnpj = '11222333000181'
				RETURNING id
				""", Long.class);
	}

	private void insertSettlement(UUID key, Long receivableId, String currency, Long rateId, String rate,
			String paymentAmount) {
		jdbcTemplate.update("""
				INSERT INTO settlement (idempotency_key, receivable_id, receivable_type, face_value, term_months,
				    base_rate, spread, present_value_brl, discount_brl, payment_currency, exchange_rate_id,
				    exchange_rate, payment_amount, settled_at, created_at)
				VALUES (?, ?, 'DUPLICATA_MERCANTIL', 100000.00, 3, 0.01, 0.015, 92859.94, 7140.06, ?, ?, ?, ?,
				    now(), now())
				""", key, receivableId, currency, rateId, rate == null ? null : new BigDecimal(rate),
				new BigDecimal(paymentAmount));
	}

}
