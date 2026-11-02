package com.srm.creditengine.domain.assignor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.infrastructure.persistence.assignor.Assignor;
import com.srm.creditengine.infrastructure.persistence.assignor.AssignorRepository;

@Service
public class AssignorService {

	private final AssignorRepository repository;

	public AssignorService(AssignorRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<Assignor> listActive() {
		return repository.findAllByActiveTrueOrderByLegalNameAsc();
	}

	@Transactional(readOnly = true)
	public Assignor requireActive(Long id) {
		Assignor assignor = repository.findById(id).orElseThrow(() -> new AssignorNotFoundException(id));
		if (!assignor.isActive()) {
			throw new InactiveAssignorException(id);
		}
		return assignor;
	}

}
