package com.srm.creditengine.api.receivable;

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
@Import({ TestcontainersConfiguration.class, ReceivableControllerTest.FixedClockConfiguration.class })
class ReceivableControllerTest {

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

	private Long alfaId;
	private Long betaId;
	private Long gamaId;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM receivable");
		alfaId = assignorId("11222333000181");
		betaId = assignorId("12ABC34501DE35");
		gamaId = assignorId("B7C8D9E0000120");
	}

	@Test
	void createReturns201WithResolvableLocation() throws Exception {
		String location = create(alfaId, "100000", "2026-04-15")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(".*/receivables/\\d+$")))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(content().string(containsString("\"faceValue\":100000.00")))
				.andExpect(jsonPath("$.createdAt").value("2026-01-15T15:00:00Z"))
				.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assignorId").value(alfaId))
				.andExpect(jsonPath("$.type").value("DUPLICATA_MERCANTIL"))
				.andExpect(jsonPath("$.dueDate").value("2026-04-15"))
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void unknownAssignorReturns422() throws Exception {
		create(999_999L, "100.00", "2026-04-15")
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value(containsString("not found")));
	}

	@Test
	void inactiveAssignorReturns422() throws Exception {
		create(gamaId, "100.00", "2026-04-15")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("inactive")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-01-15", "2026-01-14" })
	void dueDateTodayOrPastReturns422(String dueDate) throws Exception {
		create(alfaId, "100.00", dueDate)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("must be after today")));
	}

	@Test
	void faceValueWithThreeDecimalsReturns400() throws Exception {
		create(alfaId, "100.005", "2026-04-15")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("faceValue"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
			{"type": "DUPLICATA_MERCANTIL", "faceValue": 100.00, "dueDate": "2026-04-15"}""",
			"""
			{"assignorId": 1, "faceValue": 100.00, "dueDate": "2026-04-15"}""",
			"""
			{"assignorId": 1, "type": "DUPLICATA_MERCANTIL", "dueDate": "2026-04-15"}""",
			"""
			{"assignorId": 1, "type": "DUPLICATA_MERCANTIL", "faceValue": 100.00}""",
			"""
			{"assignorId": 1, "type": "DUPLICATA_MERCANTIL", "faceValue": 0, "dueDate": "2026-04-15"}""",
			"""
			{"assignorId": 1, "type": "NOTA_PROMISSORIA", "faceValue": 100.00, "dueDate": "2026-04-15"}""" })
	void invalidRequestReturns400(String body) throws Exception {
		mockMvc.perform(post("/receivables").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listFiltersByStatusAssignorAndDuplicateCandidates() throws Exception {
		create(alfaId, "100000.00", "2026-04-15").andExpect(status().isCreated());
		create(alfaId, "25000.00", "2026-03-15").andExpect(status().isCreated());
		create(betaId, "100000.00", "2026-04-15").andExpect(status().isCreated());
		insertSettled(alfaId, "5000.00", "2026-02-20");

		mockMvc.perform(get("/receivables").param("status", "SETTLED"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].dueDate").value("2026-02-20"));
		mockMvc.perform(get("/receivables").param("status", "PENDING"))
				.andExpect(jsonPath("$.totalElements").value(3));
		mockMvc.perform(get("/receivables").param("assignorId", betaId.toString()))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].assignorId").value(betaId));
		mockMvc.perform(get("/receivables").param("assignorId", alfaId.toString())
				.param("faceValue", "100000").param("dueDate", "2026-04-15"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].dueDate").value("2026-04-15"));
	}

	@Test
	void listIsPaginatedNewestFirst() throws Exception {
		create(alfaId, "100.00", "2026-04-15").andExpect(status().isCreated());
		create(alfaId, "100.00", "2026-05-15").andExpect(status().isCreated());
		create(alfaId, "100.00", "2026-06-15").andExpect(status().isCreated());

		mockMvc.perform(get("/receivables").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].dueDate").value("2026-06-15"))
				.andExpect(jsonPath("$.content[1].dueDate").value("2026-05-15"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
		mockMvc.perform(get("/receivables").param("size", "2").param("page", "1"))
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].dueDate").value("2026-04-15"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "page=-1", "size=0", "size=101", "status=CANCELLED", "dueDate=15/04/2026" })
	void invalidListParametersReturn400(String query) throws Exception {
		mockMvc.perform(get("/receivables?" + query))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void unknownIdReturns404() throws Exception {
		mockMvc.perform(get("/receivables/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Receivable not found"));
	}

	@Test
	void receivablesCannotBeUpdatedOrDeleted() throws Exception {
		mockMvc.perform(put("/receivables/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/receivables/1"))
				.andExpect(status().isMethodNotAllowed());
	}

	private ResultActions create(Long assignorId, String faceValue, String dueDate) throws Exception {
		return mockMvc.perform(post("/receivables").contentType(MediaType.APPLICATION_JSON).content("""
				{"assignorId": %d, "type": "DUPLICATA_MERCANTIL", "faceValue": %s, "dueDate": "%s"}
				""".formatted(assignorId, faceValue, dueDate)));
	}

	private void insertSettled(Long assignorId, String faceValue, String dueDate) {
		jdbcTemplate.update("""
				INSERT INTO receivable (assignor_id, type, face_value, due_date, status, created_at)
				VALUES (?, 'DUPLICATA_MERCANTIL', ?, CAST(? AS DATE), 'SETTLED', ?)
				""", assignorId, new BigDecimal(faceValue), dueDate, java.sql.Timestamp.from(NOW));
	}

	private Long assignorId(String cnpj) {
		return jdbcTemplate.queryForObject("SELECT id FROM assignor WHERE cnpj = ?", Long.class, cnpj);
	}

}
