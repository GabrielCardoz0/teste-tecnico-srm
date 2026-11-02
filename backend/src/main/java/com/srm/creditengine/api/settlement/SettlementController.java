package com.srm.creditengine.api.settlement;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.srm.creditengine.domain.settlement.SettlementOutcome;
import com.srm.creditengine.domain.settlement.SettlementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/settlements")
public class SettlementController {

	private final SettlementService service;

	public SettlementController(SettlementService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<SettlementResponse> settle(@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody SettleRequest request, UriComponentsBuilder uriBuilder) {
		SettlementOutcome outcome = service.settle(idempotencyKey, request.receivableId(),
				request.paymentCurrency());
		SettlementResponse body = SettlementResponse.from(outcome.settlement());
		if (!outcome.created()) {
			return ResponseEntity.ok(body);
		}
		URI location = uriBuilder.path("/settlements/{id}").buildAndExpand(body.id()).toUri();
		return ResponseEntity.created(location).body(body);
	}

	@GetMapping("/{id}")
	SettlementResponse findById(@PathVariable Long id) {
		return SettlementResponse.from(service.findById(id));
	}

}
