package com.srm.creditengine.infrastructure.persistence.receivable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.srm.creditengine.TestcontainersConfiguration;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReceivableSchemaTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void acceptsValidReceivable() {
		insert(alfaId(), "DUPLICATA_MERCANTIL", "100000.00", "PENDING");

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receivable", Integer.class)).isEqualTo(1);
	}

	@Test
	void rejectsNonPositiveFaceValue() {
		assertThatThrownBy(() -> insert(alfaId(), "DUPLICATA_MERCANTIL", "0.00", "PENDING"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_receivable_positive_face_value");
	}

	@Test
	void rejectsUnknownStatus() {
		assertThatThrownBy(() -> insert(alfaId(), "DUPLICATA_MERCANTIL", "100.00", "CANCELLED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_receivable_status");
	}

	@Test
	void rejectsUnknownAssignor() {
		assertThatThrownBy(() -> insert(999_999L, "DUPLICATA_MERCANTIL", "100.00", "PENDING"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("receivable_assignor_id_fkey");
	}

	private Long alfaId() {
		return jdbcTemplate.queryForObject("SELECT id FROM assignor WHERE cnpj = '11222333000181'", Long.class);
	}

	private void insert(Long assignorId, String type, String faceValue, String status) {
		jdbcTemplate.update("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				VALUES (?, ?, ?, DATE '2026-04-15', ?, now())
				""", assignorId, type, new BigDecimal(faceValue), status);
	}

}
