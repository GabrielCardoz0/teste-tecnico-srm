package com.srm.creditengine.api.pricing;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.domain.simulation.SimulationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pricing/simulations")
public class SimulationController {

	private final SimulationService service;

	public SimulationController(SimulationService service) {
		this.service = service;
	}

	@PostMapping
	SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
		return SimulationResponse.from(service.simulate(request.type(), request.faceValue(), request.dueDate(),
				request.paymentCurrency()));
	}

}
