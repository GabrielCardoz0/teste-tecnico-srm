package com.srm.creditengine.api.assignor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.domain.assignor.AssignorService;

@RestController
@RequestMapping("/assignors")
public class AssignorController {

	private final AssignorService service;

	public AssignorController(AssignorService service) {
		this.service = service;
	}

	@GetMapping
	List<AssignorResponse> listActive() {
		return service.listActive().stream().map(AssignorResponse::from).toList();
	}

}
