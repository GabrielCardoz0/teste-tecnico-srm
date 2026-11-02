package com.srm.creditengine.api.exchangerate;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.srm.creditengine.domain.exchangerate.ExchangeRateService;
import com.srm.creditengine.infrastructure.persistence.exchangerate.ExchangeRate;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/exchange-rates")
public class ExchangeRateController {

	private final ExchangeRateService service;

	public ExchangeRateController(ExchangeRateService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ExchangeRateResponse> create(@Valid @RequestBody CreateExchangeRateRequest request,
			UriComponentsBuilder uriBuilder) {
		ExchangeRate saved = service.register(request.sourceCurrency(), request.targetCurrency(), request.rate(),
				request.validFrom());
		URI location = uriBuilder.path("/exchange-rates/{id}").buildAndExpand(saved.getId()).toUri();
		return ResponseEntity.created(location).body(ExchangeRateResponse.from(saved));
	}

	@GetMapping("/current")
	ExchangeRateResponse current() {
		return ExchangeRateResponse.from(service.current());
	}

	@GetMapping("/{id}")
	ExchangeRateResponse findById(@PathVariable Long id) {
		return ExchangeRateResponse.from(service.findById(id));
	}

	@GetMapping
	List<ExchangeRateResponse> history() {
		return service.history().stream().map(ExchangeRateResponse::from).toList();
	}

}
