package com.srm.creditengine.domain.assignor;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class InactiveAssignorException extends BusinessRuleViolationException {

	public InactiveAssignorException(Long id) {
		super("Assignor " + id + " is inactive and cannot receive new receivables");
	}

}
