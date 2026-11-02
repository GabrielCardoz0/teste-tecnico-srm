package com.srm.creditengine.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.domain.exchangerate.CurrencyCode;
import com.srm.creditengine.domain.exchangerate.ExchangeRateNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import(GlobalExceptionHandlerTest.ProbeController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void invalidBodyReturns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
	}

	@Test
	void malformedJsonReturns400ProblemDetail() throws Exception {
		mockMvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void unknownRouteReturns404ProblemDetail() throws Exception {
		mockMvc.perform(get("/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
		mockMvc.perform(get("/probe/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
	}

	@Test
	void missingExchangeRateReturns404ProblemDetail() throws Exception {
		mockMvc.perform(get("/probe/missing-rate"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Exchange rate not found"))
				.andExpect(jsonPath("$.detail").value("No current USD/BRL exchange rate"));
	}

	@RestController
	static class ProbeController {

		record Request(@NotBlank String name) {
		}

		@PostMapping("/probe")
		void validated(@Valid @RequestBody Request request) {
		}

		@GetMapping("/probe/boom")
		void boom() {
			throw new IllegalStateException("secret internal detail");
		}

		@GetMapping("/probe/missing-rate")
		void missingRate() {
			throw ExchangeRateNotFoundException.noCurrentRate(CurrencyCode.USD, CurrencyCode.BRL);
		}

	}

}
