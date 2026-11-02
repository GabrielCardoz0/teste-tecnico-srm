package com.srm.creditengine.infrastructure.persistence.assignor;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface AssignorRepository extends Repository<Assignor, Long> {

	List<Assignor> findAllByActiveTrueOrderByLegalNameAsc();

}
