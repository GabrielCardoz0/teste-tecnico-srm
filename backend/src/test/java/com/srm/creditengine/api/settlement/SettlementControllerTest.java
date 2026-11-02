package com.srm.creditengine.api.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.srm.creditengine.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, SettlementControllerTest.FixedClockConfiguration.class })
class SettlementControllerTest {

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
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM settlement");
		jdbcTemplate.update("DELETE FROM receivable");
		jdbcTemplate.update("DELETE FROM exchange_rate");
	}

	@Test
	void brlSettlementMatchesGoldenCaseC1AndSettlesReceivable() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		UUID key = UUID.randomUUID();

		String location = settle(key, receivableId, "BRL")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/settlements/\\d+$")))
				.andExpect(jsonPath("$.idempotencyKey").value(key.toString()))
				.andExpect(jsonPath("$.receivableId").value(receivableId))
				.andExpect(jsonPath("$.receivableType").value("DUPLICATA_MERCANTIL"))
				.andExpect(jsonPath("$.termMonths").value(3))
				.andExpect(content().string(containsString("\"faceValue\":100000.00")))
				.andExpect(content().string(containsString("\"presentValueBrl\":92859.94")))
				.andExpect(content().string(containsString("\"discountBrl\":7140.06")))
				.andExpect(content().string(containsString("\"paymentAmount\":92859.94")))
				.andExpect(jsonPath("$.exchangeRateId").doesNotExist())
				.andExpect(jsonPath("$.settledAt").value("2026-01-15T15:00:00Z"))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location)).andExpect(status().isOk())
				.andExpect(jsonPath("$.idempotencyKey").value(key.toString()));
		assertThat(receivableStatus(receivableId)).isEqualTo("SETTLED");
	}

	@Test
	void usdSettlementMatchesGoldenCaseC3AndRecordsTheRateUsed() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		Long rateId = insertRate("5.4321", "2026-01-15T12:00:00Z");

		settle(UUID.randomUUID(), receivableId, "USD")
				.andExpect(status().isCreated())
				.andExpect(content().string(containsString("\"presentValueBrl\":92859.94")))
				.andExpect(content().string(containsString("\"discountBrl\":7140.06")))
				.andExpect(content().string(containsString("\"paymentAmount\":17094.67")))
				.andExpect(content().string(containsString("\"exchangeRate\":5.43210000")))
				.andExpect(jsonPath("$.exchangeRateId").value(rateId));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT exchange_rate_id FROM settlement WHERE receivable_id = ?", Long.class, receivableId))
				.isEqualTo(rateId);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT exchange_rate FROM settlement WHERE receivable_id = ?", BigDecimal.class, receivableId))
				.isEqualByComparingTo("5.4321");
	}

	@Test
	void retryWithSameKeyReturnsTheSameSettlement() throws Exception {
		Long receivableId = insertReceivable("CHEQUE_PRE_DATADO", "25000.00", "2026-03-15");
		UUID key = UUID.randomUUID();

		String firstBody = settle(key, receivableId, "BRL").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		settle(key, receivableId, "BRL")
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().json(firstBody, JsonCompareMode.STRICT))
				.andExpect(content().string(firstBody));
		assertThat(countSettlements()).isEqualTo(1);
	}

	@Test
	void sameKeyWithDifferentBodyReturns422() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		insertRate("5.4321", "2026-01-15T12:00:00Z");
		UUID key = UUID.randomUUID();
		settle(key, receivableId, "BRL").andExpect(status().isCreated());

		settle(key, receivableId, "USD")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("already used")));
		assertThat(countSettlements()).isEqualTo(1);
	}

	@Test
	void missingOrMalformedIdempotencyKeyReturns400() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		String body = """
				{"receivableId": %d, "paymentCurrency": "BRL"}
				""".formatted(receivableId);

		mockMvc.perform(post("/settlements").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(post("/settlements").header("Idempotency-Key", "not-a-uuid")
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		assertThat(countSettlements()).isZero();
	}

	@Test
	void invalidBodyReturns400() throws Exception {
		mockMvc.perform(post("/settlements").header("Idempotency-Key", UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON).content("{\"paymentCurrency\": \"BRL\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void alreadySettledReceivableWithNewKeyReturns409() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		settle(UUID.randomUUID(), receivableId, "BRL").andExpect(status().isCreated());

		settle(UUID.randomUUID(), receivableId, "BRL")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Receivable already settled"));
		assertThat(countSettlements()).isEqualTo(1);
	}

	@Test
	void unknownReceivableReturns422() throws Exception {
		settle(UUID.randomUUID(), 999_999L, "BRL")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("not found")));
	}

	@Test
	void overdueReceivableReturns422AndStaysPending() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-01-14");

		settle(UUID.randomUUID(), receivableId, "BRL")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("must be after today")));
		assertThat(receivableStatus(receivableId)).isEqualTo("PENDING");
	}

	@Test
	void usdWithoutCurrentRateReturns422AndReceivableStaysPending() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		insertRate("5.4321", "2026-01-15T16:00:00Z");

		settle(UUID.randomUUID(), receivableId, "USD")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("No current USD/BRL exchange rate")));
		assertThat(receivableStatus(receivableId)).isEqualTo("PENDING");
		assertThat(countSettlements()).isZero();
	}

	@Test
	void failureAfterStatusChangeRollsBackTheReceivable() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		insertOrphanSettlement(receivableId);

		settle(UUID.randomUUID(), receivableId, "BRL").andExpect(status().isConflict());

		assertThat(receivableStatus(receivableId)).isEqualTo("PENDING");
		assertThat(countSettlements()).isEqualTo(1);
	}

	@RepeatedTest(5)
	void concurrentRequestsWithSameKeyCreateASingleSettlement() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");
		UUID key = UUID.randomUUID();

		List<Integer> statuses = runConcurrently(() -> settleStatus(key, receivableId),
				() -> settleStatus(key, receivableId));

		assertThat(statuses).containsExactlyInAnyOrder(201, 200);
		assertThat(countSettlements()).isEqualTo(1);
	}

	@RepeatedTest(5)
	void concurrentRequestsWithDifferentKeysForSameReceivableCreateASingleSettlement() throws Exception {
		Long receivableId = insertReceivable("DUPLICATA_MERCANTIL", "100000.00", "2026-04-15");

		List<Integer> statuses = runConcurrently(() -> settleStatus(UUID.randomUUID(), receivableId),
				() -> settleStatus(UUID.randomUUID(), receivableId));

		assertThat(statuses).containsExactlyInAnyOrder(201, 409);
		assertThat(countSettlements()).isEqualTo(1);
	}

	@Test
	void unknownSettlementIdReturns404() throws Exception {
		mockMvc.perform(get("/settlements/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Settlement not found"));
	}

	@Test
	void settlementsCannotBeUpdatedOrDeleted() throws Exception {
		mockMvc.perform(put("/settlements/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/settlements/1"))
				.andExpect(status().isMethodNotAllowed());
	}

	@SafeVarargs
	private List<Integer> runConcurrently(Callable<Integer>... requests) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(requests.length);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Integer>> futures = java.util.Arrays.stream(requests)
					.map(request -> executor.submit(() -> {
						start.await();
						return request.call();
					}))
					.toList();
			start.countDown();
			List<Integer> statuses = new java.util.ArrayList<>();
			for (Future<Integer> future : futures) {
				statuses.add(future.get());
			}
			return statuses;
		} finally {
			executor.shutdownNow();
		}
	}

	private int settleStatus(UUID key, Long receivableId) throws Exception {
		return settle(key, receivableId, "BRL").andReturn().getResponse().getStatus();
	}

	private ResultActions settle(UUID key, Long receivableId, String paymentCurrency) throws Exception {
		return mockMvc.perform(post("/settlements").header("Idempotency-Key", key.toString())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"receivableId": %d, "paymentCurrency": "%s"}
						""".formatted(receivableId, paymentCurrency)));
	}

	private Long insertReceivable(String type, String faceValue, String dueDate) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				SELECT id, ?, ?, CAST(? AS DATE), 'PENDING', ? FROM assignor WHERE cnpj = '11222333000181'
				RETURNING id
				""", Long.class, type, new BigDecimal(faceValue), dueDate, Timestamp.from(NOW));
	}

	private Long insertRate(String rate, String validFrom) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO exchange_rate (source_currency, target_currency, rate, valid_from, created_at)
				VALUES ('USD', 'BRL', ?, ?, ?) RETURNING id
				""", Long.class, new BigDecimal(rate), Timestamp.from(Instant.parse(validFrom)), Timestamp.from(NOW));
	}

	private void insertOrphanSettlement(Long receivableId) {
		jdbcTemplate.update("""
				INSERT INTO settlement (idempotency_key, receivable_id, receivable_type, face_value, term_months,
				    base_rate, spread, present_value_brl, discount_brl, payment_currency, payment_amount,
				    settled_at, created_at)
				VALUES (?, ?, 'DUPLICATA_MERCANTIL', 100000.00, 3, 0.01, 0.015, 92859.94, 7140.06, 'BRL', 92859.94,
				    ?, ?)
				""", UUID.randomUUID(), receivableId, Timestamp.from(NOW), Timestamp.from(NOW));
	}

	private String receivableStatus(Long receivableId) {
		return jdbcTemplate.queryForObject("SELECT status FROM receivable WHERE id = ?", String.class, receivableId);
	}

	private int countSettlements() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM settlement", Integer.class);
	}

}
