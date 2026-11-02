package com.srm.creditengine.domain.pricing;

import java.time.LocalDate;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class InvalidTermException extends BusinessRuleViolationException {

	public InvalidTermException(LocalDate dueDate, LocalDate today) {
		super("Due date " + dueDate + " must be after today (" + today + ")");
	}

}
