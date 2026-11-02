package com.srm.creditengine.api.assignor;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.infrastructure.persistence.assignor.AssignorRepository;

@RestController
@RequestMapping("/assignors")
public class AssignorController {

	private final AssignorRepository repository;

	public AssignorController(AssignorRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	List<AssignorResponse> listActive() {
		return repository.findAllByActiveTrueOrderByLegalNameAsc().stream().map(AssignorResponse::from).toList();
	}

}
