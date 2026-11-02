package com.srm.creditengine.domain.settlement;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class UnknownReceivableException extends BusinessRuleViolationException {

	public UnknownReceivableException(Long receivableId) {
		super("Receivable " + receivableId + " not found");
	}

}
