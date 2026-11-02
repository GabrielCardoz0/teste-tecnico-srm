package com.srm.creditengine.api.settlement;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.srm.creditengine.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SettlementStatementControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private Long alfaId;
	private Long betaId;
	private Long gamaId;
	private Long rateId;

	private int beforeFrom;
	private int fromStart;
	private int betaUsdMidPeriod;
	private int toLastMinute;
	private int afterTo;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM settlement");
		jdbcTemplate.update("DELETE FROM receivable");
		jdbcTemplate.update("DELETE FROM exchange_rate");
		alfaId = assignorId("11222333000181");
		betaId = assignorId("12ABC34501DE35");
		gamaId = assignorId("B7C8D9E0000120");
		rateId = jdbcTemplate.queryForObject("""
				INSERT INTO exchange_rate (source_currency, target_currency, rate, valid_from, created_at)
				VALUES ('USD', 'BRL', 5.4321, ?, ?) RETURNING id
				""", Long.class, Timestamp.from(Instant.parse("2026-10-01T12:00:00Z")),
				Timestamp.from(Instant.parse("2026-10-01T12:00:00Z")));

		beforeFrom = insertSettlement(alfaId, "BRL", "2026-10-31T23:59:59-03:00");
		fromStart = insertSettlement(alfaId, "BRL", "2026-11-01T00:00:00-03:00");
		betaUsdMidPeriod = insertSettlement(betaId, "USD", "2026-11-01T12:00:00-03:00");
		toLastMinute = insertSettlement(betaId, "BRL", "2026-11-02T23:59:00-03:00");
		afterTo = insertSettlement(alfaId, "USD", "2026-11-03T00:00:00-03:00");
	}

	@Test
	void periodUsesSaoPauloCalendarDaysWithInclusiveEndDay() throws Exception {
		mockMvc.perform(get("/settlements").param("from", "2026-11-01").param("to", "2026-11-02"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(toLastMinute, betaUsdMidPeriod, fromStart)));
	}

	@Test
	void singleDayPeriod() throws Exception {
		mockMvc.perform(get("/settlements").param("from", "2026-11-02").param("to", "2026-11-02"))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(toLastMinute)));
	}

	@Test
	void openEndedPeriods() throws Exception {
		mockMvc.perform(get("/settlements").param("from", "2026-11-01"))
				.andExpect(jsonPath("$.content[*].settlementId")
						.value(contains(afterTo, toLastMinute, betaUsdMidPeriod, fromStart)));
		mockMvc.perform(get("/settlements").param("to", "2026-10-31"))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(beforeFrom)));
	}

	@Test
	void filtersByAssignorAndCurrency() throws Exception {
		mockMvc.perform(get("/settlements").param("assignorId", alfaId.toString()))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(afterTo, fromStart, beforeFrom)));
		mockMvc.perform(get("/settlements").param("paymentCurrency", "USD"))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(afterTo, betaUsdMidPeriod)));
	}

	@Test
	void combinedFiltersReturnTheFullLine() throws Exception {
		mockMvc.perform(get("/settlements").param("from", "2026-11-01").param("to", "2026-11-02")
				.param("assignorId", betaId.toString()).param("paymentCurrency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].settlementId").value(betaUsdMidPeriod))
				.andExpect(jsonPath("$.content[0].settledAt").value("2026-11-01T15:00:00Z"))
				.andExpect(jsonPath("$.content[0].assignorLegalName").value("Beta Distribuidora de Alimentos S.A."))
				.andExpect(jsonPath("$.content[0].assignorCnpj").value("12ABC34501DE35"))
				.andExpect(jsonPath("$.content[0].receivableType").value("DUPLICATA_MERCANTIL"))
				.andExpect(jsonPath("$.content[0].paymentCurrency").value("USD"))
				.andExpect(content().string(containsString("\"faceValue\":100000.00")))
				.andExpect(content().string(containsString("\"presentValueBrl\":92859.94")))
				.andExpect(content().string(containsString("\"discountBrl\":7140.06")))
				.andExpect(content().string(containsString("\"exchangeRate\":5.43210000")))
				.andExpect(content().string(containsString("\"paymentAmount\":17094.67")));
	}

	@Test
	void brlLinesOmitTheExchangeRate() throws Exception {
		mockMvc.perform(get("/settlements").param("paymentCurrency", "BRL").param("size", "1"))
				.andExpect(jsonPath("$.content[0].paymentCurrency").value("BRL"))
				.andExpect(jsonPath("$.content[0].exchangeRate").doesNotExist());
	}

	@Test
	void paginatesNewestFirst() throws Exception {
		mockMvc.perform(get("/settlements").param("size", "2"))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(afterTo, toLastMinute)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(3));
		mockMvc.perform(get("/settlements").param("size", "2").param("page", "2"))
				.andExpect(jsonPath("$.content[*].settlementId").value(contains(beforeFrom)));
	}

	@Test
	void emptyResult() throws Exception {
		mockMvc.perform(get("/settlements").param("assignorId", gamaId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0))
				.andExpect(jsonPath("$.totalPages").value(0));
	}

	@Test
	void fromAfterToReturns400() throws Exception {
		mockMvc.perform(get("/settlements").param("from", "2026-11-03").param("to", "2026-11-02"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Invalid statement period"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "paymentCurrency=EUR", "from=01/11/2026", "to=2026-13-01", "assignorId=abc",
			"size=0", "size=101", "page=-1" })
	void invalidParametersReturn400(String query) throws Exception {
		mockMvc.perform(get("/settlements?" + query))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private int insertSettlement(Long assignorId, String currency, String settledAt) {
		Timestamp moment = Timestamp.from(OffsetDateTime.parse(settledAt).toInstant());
		Long receivableId = jdbcTemplate.queryForObject("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				VALUES (?, 'DUPLICATA_MERCANTIL', 100000.00, DATE '2027-01-15', 'SETTLED', ?) RETURNING id
				""", Long.class, assignorId, moment);
		boolean usd = "USD".equals(currency);
		return jdbcTemplate.queryForObject("""
				INSERT INTO settlement (idempotency_key, receivable_id, receivable_type, face_value, term_months,
				    base_rate, spread, present_value_brl, discount_brl, payment_currency, exchange_rate_id,
				    exchange_rate, payment_amount, settled_at, created_at)
				VALUES (?, ?, 'DUPLICATA_MERCANTIL', 100000.00, 3, 0.01, 0.015, 92859.94, 7140.06, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Integer.class, UUID.randomUUID(), receivableId, currency, usd ? rateId : null,
				usd ? new BigDecimal("5.43210000") : null, usd ? new BigDecimal("17094.67") : new BigDecimal("92859.94"),
				moment, moment);
	}

	private Long assignorId(String cnpj) {
		return jdbcTemplate.queryForObject("SELECT id FROM assignor WHERE cnpj = ?", Long.class, cnpj);
	}

}
