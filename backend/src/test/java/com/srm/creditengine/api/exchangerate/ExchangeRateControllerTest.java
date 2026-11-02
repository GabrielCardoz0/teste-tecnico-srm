package com.srm.creditengine.api.exchangerate;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.srm.creditengine.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ExchangeRateControllerTest.FixedClockConfiguration.class })
class ExchangeRateControllerTest {

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
	void cleanTable() {
		jdbcTemplate.update("DELETE FROM exchange_rate");
	}

	@Test
	void createReturns201WithResolvableLocation() throws Exception {
		String location = mockMvc.perform(post("/exchange-rates").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": 5.4321}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/exchange-rates/\\d+$")))
				.andExpect(jsonPath("$.validFrom").value("2026-01-15T15:00:00Z"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-15T15:00:00Z"))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sourceCurrency").value("USD"))
				.andExpect(jsonPath("$.targetCurrency").value("BRL"))
				.andExpect(jsonPath("$.rate").value(new BigDecimal("5.4321")));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
			{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": 0}""",
			"""
			{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": -5.4321}""",
			"""
			{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": 5.123456789}""",
			"""
			{"sourceCurrency": "USD", "targetCurrency": "BRL"}""",
			"""
			{"sourceCurrency": "BRL", "targetCurrency": "USD", "rate": 0.18}""",
			"""
			{"sourceCurrency": "USD", "targetCurrency": "USD", "rate": 1}""",
			"""
			{"sourceCurrency": "EUR", "targetCurrency": "BRL", "rate": 6.1}""" })
	void invalidRequestReturns400(String body) throws Exception {
		mockMvc.perform(post("/exchange-rates").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void currentReturns404WhenThereIsNoRate() throws Exception {
		mockMvc.perform(get("/exchange-rates/current"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void currentReturnsMostRecentValidRateIgnoringFuture() throws Exception {
		register("5.1000", "2026-01-14T15:00:00Z");
		register("5.2000", "2026-01-15T14:00:00Z");
		register("5.9000", "2026-01-15T16:00:00Z");

		mockMvc.perform(get("/exchange-rates/current"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(new BigDecimal("5.2")))
				.andExpect(jsonPath("$.validFrom").value("2026-01-15T14:00:00Z"));
	}

	@Test
	void historyIsOrderedByValidFromDescending() throws Exception {
		register("5.1000", "2026-01-14T15:00:00Z");
		register("5.9000", "2026-01-15T16:00:00Z");
		register("5.2000", "2026-01-15T14:00:00Z");

		mockMvc.perform(get("/exchange-rates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].validFrom").value("2026-01-15T16:00:00Z"))
				.andExpect(jsonPath("$[1].validFrom").value("2026-01-15T14:00:00Z"))
				.andExpect(jsonPath("$[2].validFrom").value("2026-01-14T15:00:00Z"));
	}

	@Test
	void unknownIdReturns404() throws Exception {
		mockMvc.perform(get("/exchange-rates/999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void ratesCannotBeUpdatedOrDeleted() throws Exception {
		mockMvc.perform(put("/exchange-rates/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/exchange-rates/1"))
				.andExpect(status().isMethodNotAllowed());
	}

	private void register(String rate, String validFrom) throws Exception {
		mockMvc.perform(post("/exchange-rates").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": %s, "validFrom": "%s"}
						""".formatted(rate, validFrom)))
				.andExpect(status().isCreated());
	}

}
