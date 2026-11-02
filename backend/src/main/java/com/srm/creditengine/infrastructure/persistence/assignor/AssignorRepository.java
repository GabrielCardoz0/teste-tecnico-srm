package com.srm.creditengine.infrastructure.persistence.assignor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface AssignorRepository extends Repository<Assignor, Long> {

	Optional<Assignor> findById(Long id);

	List<Assignor> findAllByActiveTrueOrderByLegalNameAsc();

}
