package com.srm.creditengine.infrastructure.persistence.assignor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class AssignorSchemaTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void seedHasThreeActiveAndOneInactiveAssignor() {
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM assignor WHERE active", Integer.class))
				.isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM assignor WHERE NOT active", Integer.class))
				.isEqualTo(1);
	}

	@Test
	void acceptsAlphanumericCnpj() {
		insert("Z9Y8X7W6000193");

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM assignor", Integer.class)).isEqualTo(5);
	}

	@ParameterizedTest
	@ValueSource(strings = { "12abc34501de35", "12ABC34501DEAB", "1122233300018", "11222333/00018" })
	void rejectsCnpjOutOfFormat(String cnpj) {
		assertThatThrownBy(() -> insert(cnpj))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_assignor_cnpj_format");
	}

	@Test
	void rejectsMaskedCnpj() {
		assertThatThrownBy(() -> insert("11.222.333/0001-81")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateCnpj() {
		assertThatThrownBy(() -> insert("11222333000181")).isInstanceOf(DuplicateKeyException.class);
	}

	private void insert(String cnpj) {
		jdbcTemplate.update("INSERT INTO assignor (cnpj, legal_name) VALUES (?, ?)", cnpj, "Test Assignor Ltda");
	}

}
