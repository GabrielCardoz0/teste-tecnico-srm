package com.srm.creditengine.api.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.srm.creditengine.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, SimulationControllerTest.FixedClockConfiguration.class })
class SimulationControllerTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo"));
		}

	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanExchangeRates() {
		jdbcTemplate.update("DELETE FROM exchange_rate");
	}

	@Test
	void goldenCaseC1() throws Exception {
		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "BRL")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termMonths").value(3))
				.andExpect(jsonPath("$.paymentCurrency").value("BRL"))
				.andExpect(content().string(containsString("\"baseRate\":0.01")))
				.andExpect(content().string(containsString("\"spread\":0.015")))
				.andExpect(content().string(containsString("\"presentValueBrl\":92859.94")))
				.andExpect(content().string(containsString("\"discountBrl\":7140.06")))
				.andExpect(jsonPath("$.exchangeRate").doesNotExist())
				.andExpect(jsonPath("$.amountUsd").doesNotExist());
	}

	@Test
	void goldenCaseC2() throws Exception {
		simulate("CHEQUE_PRE_DATADO", "25000.00", "2026-03-15", "BRL")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termMonths").value(2))
				.andExpect(content().string(containsString("\"spread\":0.025")))
				.andExpect(content().string(containsString("\"presentValueBrl\":23337.77")))
				.andExpect(content().string(containsString("\"discountBrl\":1662.23")));
	}

	@Test
	void goldenCaseC3() throws Exception {
		Long rateId = insertRate("5.4321", "2026-01-15T12:00:00Z");

		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "USD")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.termMonths").value(3))
				.andExpect(jsonPath("$.paymentCurrency").value("USD"))
				.andExpect(content().string(containsString("\"presentValueBrl\":92859.94")))
				.andExpect(content().string(containsString("\"discountBrl\":7140.06")))
				.andExpect(content().string(containsString("\"amountUsd\":17094.67")))
				.andExpect(jsonPath("$.exchangeRate.id").value(rateId))
				.andExpect(content().string(containsString("\"rate\":5.43210000")))
				.andExpect(jsonPath("$.exchangeRate.validFrom").value("2026-01-15T12:00:00Z"));
	}

	@Test
	void usdUsesOnlyTheCurrentRate() throws Exception {
		insertRate("5.0000", "2026-01-10T12:00:00Z");
		Long currentId = insertRate("5.4321", "2026-01-15T12:00:00Z");
		insertRate("9.9999", "2026-01-15T16:00:00Z");

		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "USD")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exchangeRate.id").value(currentId));
	}

	@Test
	void usdWithoutCurrentRateReturns422() throws Exception {
		insertRate("5.4321", "2026-01-15T16:00:00Z");

		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "USD")
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString("No current USD/BRL exchange rate")));
	}

	@Test
	void dueDateTodayReturns422() throws Exception {
		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-01-15", "BRL")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("must be after today")));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
			{"faceValue": 100.00, "dueDate": "2026-04-15", "paymentCurrency": "BRL"}""",
			"""
			{"type": "DUPLICATA_MERCANTIL", "dueDate": "2026-04-15", "paymentCurrency": "BRL"}""",
			"""
			{"type": "DUPLICATA_MERCANTIL", "faceValue": 100.00, "paymentCurrency": "BRL"}""",
			"""
			{"type": "DUPLICATA_MERCANTIL", "faceValue": 100.00, "dueDate": "2026-04-15"}""",
			"""
			{"type": "DUPLICATA_MERCANTIL", "faceValue": 100.005, "dueDate": "2026-04-15", "paymentCurrency": "BRL"}""",
			"""
			{"type": "DUPLICATA_MERCANTIL", "faceValue": 100.00, "dueDate": "2026-04-15", "paymentCurrency": "EUR"}""" })
	void invalidRequestReturns400(String body) throws Exception {
		mockMvc.perform(post("/pricing/simulations").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void simulationDoesNotPersistAnything() throws Exception {
		insertRate("5.4321", "2026-01-15T12:00:00Z");
		int receivablesBefore = countReceivables();
		int ratesBefore = countExchangeRates();

		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "BRL").andExpect(status().isOk());
		simulate("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15", "USD").andExpect(status().isOk());

		assertThat(countReceivables()).isEqualTo(receivablesBefore);
		assertThat(countExchangeRates()).isEqualTo(ratesBefore);
	}

	private ResultActions simulate(String type, String faceValue, String dueDate, String paymentCurrency)
			throws Exception {
		return mockMvc.perform(post("/pricing/simulations").contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "%s", "faceValue": %s, "dueDate": "%s", "paymentCurrency": "%s"}
				""".formatted(type, faceValue, dueDate, paymentCurrency)));
	}

	private Long insertRate(String rate, String validFrom) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO exchange_rate (source_currency, target_currency, rate, valid_from, created_at)
				VALUES ('USD', 'BRL', ?, ?, ?) RETURNING id
				""", Long.class, new BigDecimal(rate), Timestamp.from(Instant.parse(validFrom)), Timestamp.from(NOW));
	}

	private int countReceivables() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM receivable", Integer.class);
	}

	private int countExchangeRates() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM exchange_rate", Integer.class);
	}

}
