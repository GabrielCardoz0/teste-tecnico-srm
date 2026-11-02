package com.srm.creditengine.domain.assignor;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class AssignorNotFoundException extends BusinessRuleViolationException {

	public AssignorNotFoundException(Long id) {
		super("Assignor " + id + " not found");
	}

}
