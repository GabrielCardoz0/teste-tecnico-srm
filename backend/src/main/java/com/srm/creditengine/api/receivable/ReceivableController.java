package com.srm.creditengine.api.receivable;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.srm.creditengine.api.PageResponse;
import com.srm.creditengine.domain.receivable.ReceivableService;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/receivables")
public class ReceivableController {

	private final ReceivableService service;

	public ReceivableController(ReceivableService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ReceivableResponse> create(@Valid @RequestBody CreateReceivableRequest request,
			UriComponentsBuilder uriBuilder) {
		Receivable saved = service.register(request.assignorId(), request.type(), request.faceValue(),
				request.dueDate());
		URI location = uriBuilder.path("/receivables/{id}").buildAndExpand(saved.getId()).toUri();
		return ResponseEntity.created(location).body(ReceivableResponse.from(saved));
	}

	@GetMapping("/{id}")
	ReceivableResponse findById(@PathVariable Long id) {
		return ReceivableResponse.from(service.findById(id));
	}

	@GetMapping
	PageResponse<ReceivableResponse> search(
			@RequestParam(required = false) ReceivableStatus status,
			@RequestParam(required = false) Long assignorId,
			@RequestParam(required = false) BigDecimal faceValue,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return PageResponse.from(service.search(status, assignorId, faceValue, dueDate, page, size),
				ReceivableResponse::from);
	}

}
